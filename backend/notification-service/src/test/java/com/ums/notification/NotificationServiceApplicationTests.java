package com.ums.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:notification_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.flyway.enabled=false",
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"spring.mail.host=localhost",
		"spring.mail.port=2525",
		"management.health.rabbit.enabled=false",
		"management.health.mail.enabled=false",
		"notification.retry.initial-delay-ms=600000",
		"notification.retry.fixed-delay-ms=600000",
		"internal.gateway.secret=test-gateway-secret",
		"internal.service.secret=test-internal-service-secret"
})
@AutoConfigureMockMvc
class NotificationServiceApplicationTests {

	private static final String GATEWAY_SECRET_HEADER = "X-Internal-Gateway-Secret";
	private static final String INTERNAL_SERVICE_SECRET_HEADER = "X-Internal-Service-Secret";
	private static final String AUTHENTICATED_USER_HEADER = "X-Authenticated-User";
	private static final String USER_ROLES_HEADER = "X-User-Roles";
	private static final String TEST_GATEWAY_SECRET = "test-gateway-secret";
	private static final String TEST_INTERNAL_SERVICE_SECRET = "test-internal-service-secret";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void healthEndpointRemainsPublic() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}

	@Test
	void notificationLogsRejectDirectJwtWithoutGatewayTrust() throws Exception {
		mockMvc.perform(get("/api/v1/notifications")
				.header("Authorization", "Bearer not-a-real-token"))
				.andExpect(status().isForbidden());
	}

	@Test
	void notificationLogsRejectSpoofedGatewayIdentityWithoutSecret() throws Exception {
		mockMvc.perform(get("/api/v1/notifications")
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header(USER_ROLES_HEADER, "SUPER_ADMIN"))
				.andExpect(status().isForbidden());
	}

	@Test
	void notificationLogsRequireSupportOrAdminAuthority() throws Exception {
		mockMvc.perform(get("/api/v1/notifications")
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header(GATEWAY_SECRET_HEADER, TEST_GATEWAY_SECRET)
				.header(USER_ROLES_HEADER, "MEMBER"))
				.andExpect(status().isForbidden());
	}

	@Test
	void notificationLogsAcceptTrustedSupportUser() throws Exception {
		mockMvc.perform(get("/api/v1/notifications")
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header(GATEWAY_SECRET_HEADER, TEST_GATEWAY_SECRET)
				.header(USER_ROLES_HEADER, "SUPPORT"))
				.andExpect(status().isOk());
	}

	@Test
	void templateCreateRequiresAdminAuthority() throws Exception {
		mockMvc.perform(post("/api/v1/templates")
				.contentType("application/json")
				.content(templateRequestBody("SUPPORT_TEMPLATE"))
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header(GATEWAY_SECRET_HEADER, TEST_GATEWAY_SECRET)
				.header(USER_ROLES_HEADER, "SUPPORT"))
				.andExpect(status().isForbidden());
	}

	@Test
	void templateCreateAcceptsTrustedAdminUser() throws Exception {
		mockMvc.perform(post("/api/v1/templates")
				.contentType("application/json")
				.content(templateRequestBody("ADMIN_TEMPLATE"))
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header(GATEWAY_SECRET_HEADER, TEST_GATEWAY_SECRET)
				.header(USER_ROLES_HEADER, "NOTIFICATION_ADMIN"))
				.andExpect(status().isCreated());
	}

	@Test
	void testEndpointIsNotPublicEvenInDevProfile() throws Exception {
		mockMvc.perform(get("/api/test/welcome"))
				.andExpect(status().isForbidden());
	}

	@Test
	void internalRouteRejectsGatewaySecretOnly() throws Exception {
		mockMvc.perform(get("/api/v1/internal/notifications/ping")
				.header(GATEWAY_SECRET_HEADER, TEST_GATEWAY_SECRET))
				.andExpect(status().isForbidden());
	}

	@Test
	void internalRouteAcceptsInternalServiceSecret() throws Exception {
		mockMvc.perform(get("/api/v1/internal/notifications/ping")
				.header(INTERNAL_SERVICE_SECRET_HEADER, TEST_INTERNAL_SERVICE_SECRET))
				.andExpect(status().isNotFound());
	}

	private String templateRequestBody(String templateCode) {
		return """
				{
				  "templateCode": "%s",
				  "subject": "Test subject",
				  "body": "Hello {{name}}",
				  "channel": "EMAIL"
				}
				""".formatted(templateCode);
	}
}
