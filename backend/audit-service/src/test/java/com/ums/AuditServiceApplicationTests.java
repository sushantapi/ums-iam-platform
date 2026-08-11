package com.ums;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ums.entity.AuditLog;
import com.ums.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:audit_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.flyway.enabled=false",
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"management.health.rabbit.enabled=false",
		"internal.gateway.secret=test-gateway-secret",
		"internal.service.secret=test-internal-service-secret"
})
@AutoConfigureMockMvc
class AuditServiceApplicationTests {

	private static final String GATEWAY_SECRET_HEADER = "X-Internal-Gateway-Secret";
	private static final String INTERNAL_SERVICE_SECRET_HEADER = "X-Internal-Service-Secret";
	private static final String AUTHENTICATED_USER_HEADER = "X-Authenticated-User";
	private static final String USER_ROLES_HEADER = "X-User-Roles";
	private static final String USER_PERMISSIONS_HEADER = "X-User-Permissions";
	private static final String TEST_GATEWAY_SECRET = "test-gateway-secret";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuditLogRepository repository;

	@Test
	void contextLoads() {
	}

	@Test
	void healthAndInfoEndpointsRemainPublic() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/actuator/info"))
				.andExpect(status().isOk());
	}

	@Test
	void auditEventsRejectDirectJwtWithoutGatewayTrust() throws Exception {
		mockMvc.perform(get("/api/v1/audit/events")
				.header("Authorization", "Bearer not-a-real-token"))
				.andExpect(status().isForbidden());
	}

	@Test
	void auditEventsRejectSpoofedGatewayIdentityWithoutSecret() throws Exception {
		mockMvc.perform(get("/api/v1/audit/events")
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header(USER_ROLES_HEADER, "SUPER_ADMIN"))
				.andExpect(status().isForbidden());
	}

	@Test
	void internalServiceSecretDoesNotAuthorizeExternalAuditRoutes() throws Exception {
		mockMvc.perform(get("/api/v1/audit/events")
				.header(INTERNAL_SERVICE_SECRET_HEADER, "test-internal-service-secret")
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header(USER_ROLES_HEADER, "SUPER_ADMIN"))
				.andExpect(status().isForbidden());
	}

	@Test
	void internalAuditRouteAcceptsOnlyInternalServiceSecret() throws Exception {
		mockMvc.perform(get("/api/v1/internal/audit/events")
				.header(GATEWAY_SECRET_HEADER, TEST_GATEWAY_SECRET))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/internal/audit/events")
				.header(INTERNAL_SERVICE_SECRET_HEADER, "test-internal-service-secret"))
				.andExpect(status().isOk());
	}

	@Test
	void auditEventsRejectTrustedCallerWithoutAuditAuthority() throws Exception {
		mockMvc.perform(trustedAuditRequest()
				.header(USER_ROLES_HEADER, "ORG_ADMIN"))
				.andExpect(status().isForbidden());
	}

	@Test
	void supportDoesNotReceiveBlanketAuditAccess() throws Exception {
		mockMvc.perform(trustedAuditRequest()
				.header(USER_ROLES_HEADER, "SUPPORT"))
				.andExpect(status().isForbidden());
	}

	@Test
	void auditEventsAcceptTrustedSuperAdmin() throws Exception {
		mockMvc.perform(trustedAuditRequest()
				.header(USER_ROLES_HEADER, "SUPER_ADMIN"))
				.andExpect(status().isOk());
	}

	@Test
	void auditEventsAcceptExplicitAuditReadPermission() throws Exception {
		mockMvc.perform(trustedAuditRequest()
				.header(USER_PERMISSIONS_HEADER, "AUDIT_READ"))
				.andExpect(status().isOk());
	}

	@Test
	void auditEventDetailUsesSameAuthorizationBoundary() throws Exception {
		mockMvc.perform(get("/api/v1/audit/events/1")
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header(GATEWAY_SECRET_HEADER, TEST_GATEWAY_SECRET)
				.header(USER_ROLES_HEADER, "AUDIT_ADMIN"))
				.andExpect(status().isNotFound());
	}

	@Test
	void queryBoundsAndDateRangeAreValidated() throws Exception {
		mockMvc.perform(trustedAuditRequest()
				.header(USER_ROLES_HEADER, "SUPER_ADMIN")
				.param("size", "201"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(trustedAuditRequest()
				.header(USER_ROLES_HEADER, "SUPER_ADMIN")
				.param("from", "2026-06-24")
				.param("to", "2026-06-23"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(trustedAuditRequest()
				.header(USER_ROLES_HEADER, "SUPER_ADMIN")
				.param("outcome", "UNKNOWN"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void unsupportedOrganizationFilterIsRejectedInsteadOfIgnored() throws Exception {
		mockMvc.perform(trustedAuditRequest()
				.header(USER_ROLES_HEADER, "SUPER_ADMIN")
				.param("organizationId", UUID.randomUUID().toString()))
				.andExpect(status().isBadRequest());
	}

	@Test
	void textFilterWildcardsAreTreatedAsLiteralInput() throws Exception {
		repository.deleteAll();
		repository.save(AuditLog.builder()
				.eventType("auth.login.succeeded")
				.serviceName("authentication-service")
				.entityId("user_123")
				.createdAt(LocalDateTime.now())
				.build());

		mockMvc.perform(trustedAuditRequest()
				.header(USER_ROLES_HEADER, "SUPER_ADMIN")
				.param("target", "%"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder trustedAuditRequest() {
		return get("/api/v1/audit/events")
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header(GATEWAY_SECRET_HEADER, TEST_GATEWAY_SECRET);
	}
}
