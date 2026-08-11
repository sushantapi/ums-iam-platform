package com.ums.org;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ums.org.service.OrganizationService;

@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:organization_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=none",
		"management.health.rabbit.enabled=false",
		"internal.gateway.secret=test-gateway-secret",
		"internal.service.secret=test-internal-service-secret"
})
@AutoConfigureMockMvc
class OrganizationServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrganizationService organizationService;

	@Test
	void contextLoads() {
	}

	@Test
	void externalRouteRejectsDirectJwtAndSpoofedGatewayIdentity() throws Exception {
		String organizationId = "00000000-0000-0000-0000-000000000010";
		mockMvc.perform(get("/api/v1/organizations/{id}", organizationId)
				.header("Authorization", "Bearer not-a-real-token"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/organizations/{id}", organizationId)
				.header("X-Authenticated-User", "00000000-0000-0000-0000-000000000001"))
				.andExpect(status().isForbidden());
	}

	@Test
	void trustedGatewayIdentityAndHealthEndpointsAreAccepted() throws Exception {
		when(organizationService.getOrganization(any(), any(), anyBoolean())).thenReturn(null);
		mockMvc.perform(get("/api/v1/organizations/{id}", "00000000-0000-0000-0000-000000000010")
				.header("X-Authenticated-User", "00000000-0000-0000-0000-000000000001")
				.header("X-Internal-Gateway-Secret", "test-gateway-secret"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
	}

	@Test
	void swaggerIsNotPublic() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isForbidden());
	}

}
