package com.ums.authorization;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ums.authorization.service.AuthorizationService;

@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:authorization_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.flyway.enabled=false",
		"management.health.rabbit.enabled=false",
		"internal.gateway.secret=test-gateway-secret",
		"internal.service.secret=test-internal-service-secret"
})
@AutoConfigureMockMvc
class AuthorizationServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthorizationService authorizationService;

	@Test
	void contextLoads() {
	}

	@Test
	void internalRoleAssignmentRequiresInternalServiceSecret() throws Exception {
		when(authorizationService.assignRole(any())).thenReturn("Role assigned successfully");
		String body = """
				{"userId":"00000000-0000-0000-0000-000000000002",
				 "roleName":"EMPLOYEE","scopeType":"PLATFORM","scopeId":"*",
				 "assignedBy":"00000000-0000-0000-0000-000000000001"}
				""";

		mockMvc.perform(post("/api/v1/internal/roles/assign")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/internal/roles/assign")
				.header("X-Internal-Service-Secret", "test-internal-service-secret")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isOk());
	}

	@Test
	void externalRouteRejectsDirectJwtAndSpoofedGatewayIdentity() throws Exception {
		mockMvc.perform(get("/api/v1/roles")
				.header("Authorization", "Bearer not-a-real-token"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/roles")
				.header("X-Authenticated-User", "00000000-0000-0000-0000-000000000001")
				.header("X-User-Roles", "SUPER_ADMIN"))
				.andExpect(status().isForbidden());
	}

	@Test
	void trustedGatewayIdentityCanAccessAuthorizedExternalRoute() throws Exception {
		mockMvc.perform(get("/api/v1/roles")
				.header("X-Authenticated-User", "00000000-0000-0000-0000-000000000001")
				.header("X-Internal-Gateway-Secret", "test-gateway-secret")
				.header("X-User-Roles", "SUPER_ADMIN"))
				.andExpect(status().isOk());
	}

	@Test
	void internalRouteRejectsGatewaySecretAndDirectJwt() throws Exception {
		mockMvc.perform(post("/api/v1/internal/roles/assign")
				.header("X-Internal-Gateway-Secret", "test-gateway-secret")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/internal/roles/assign")
				.header("Authorization", "Bearer not-a-real-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void healthAndInfoArePublicWhileTestAndSwaggerRoutesAreClosed() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
		mockMvc.perform(get("/api/test/role")).andExpect(status().isForbidden());
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isForbidden());
	}

}
