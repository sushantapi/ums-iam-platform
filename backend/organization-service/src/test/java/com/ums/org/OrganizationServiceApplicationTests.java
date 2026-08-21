package com.ums.org;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ums.org.dto.OrganizationInvitationAcceptanceResponse;
import com.ums.org.enums.OrganizationInvitationStatus;
import com.ums.org.enums.OrganizationRole;
import com.ums.org.service.OrganizationInvitationAcceptanceService;
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

	private static final UUID AUTHENTICATED_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrganizationService organizationService;

	@MockitoBean
	private OrganizationInvitationAcceptanceService invitationAcceptanceService;

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
				.header("X-Authenticated-User", AUTHENTICATED_USER_ID.toString()))
				.andExpect(status().isForbidden());
	}

	@Test
	void trustedGatewayIdentityAndHealthEndpointsAreAccepted() throws Exception {
		when(organizationService.getOrganization(any(), any(), anyBoolean())).thenReturn(null);
		mockMvc.perform(get("/api/v1/organizations/{id}", "00000000-0000-0000-0000-000000000010")
				.header("X-Authenticated-User", AUTHENTICATED_USER_ID.toString())
				.header("X-Internal-Gateway-Secret", "test-gateway-secret"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
	}

	@Test
	void invitationAcceptanceRejectsUntrustedIdentity() throws Exception {
		String requestBody = "{\"token\":\"opaque-invitation-token\"}";
		mockMvc.perform(post("/api/v1/organizations/invitations/accept")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/organizations/invitations/accept")
				.header("X-Authenticated-User", AUTHENTICATED_USER_ID.toString())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isForbidden());
	}

	@Test
	void trustedGatewayCanReachInvitationAcceptance() throws Exception {
		UUID invitationId = UUID.fromString("00000000-0000-0000-0000-000000000020");
		UUID organizationId = UUID.fromString("00000000-0000-0000-0000-000000000021");
		UUID membershipId = UUID.fromString("00000000-0000-0000-0000-000000000022");
		when(invitationAcceptanceService.acceptInvitation(anyString(), any()))
				.thenReturn(new OrganizationInvitationAcceptanceResponse(
						invitationId, organizationId, membershipId, OrganizationRole.MEMBER,
						OrganizationInvitationStatus.ACCEPTED, LocalDateTime.now()));

		mockMvc.perform(post("/api/v1/organizations/invitations/accept")
				.header("X-Authenticated-User", AUTHENTICATED_USER_ID.toString())
				.header("X-Internal-Gateway-Secret", "test-gateway-secret")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"opaque-invitation-token\"}"))
				.andExpect(status().isOk());

		verify(invitationAcceptanceService).acceptInvitation("opaque-invitation-token", AUTHENTICATED_USER_ID);
	}

	@Test
	void swaggerIsNotPublic() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isForbidden());
	}
}
