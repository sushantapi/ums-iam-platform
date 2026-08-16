package com.ums.user.user_service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.ums.user.UserServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = UserServiceApplication.class, properties = {
		"spring.cloud.config.enabled=false",
		"eureka.client.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:user_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.flyway.enabled=false",
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"internal.gateway.secret=test-gateway-secret",
		"internal.service.secret=test-internal-service-secret"
})
@AutoConfigureMockMvc
class UserServiceApplicationTests {

	private static final String GATEWAY_SECRET_HEADER = "X-Internal-Gateway-Secret";
	private static final String INTERNAL_SERVICE_SECRET_HEADER = "X-Internal-Service-Secret";
	private static final String AUTHENTICATED_USER_HEADER = "X-Authenticated-User";
	private static final String TEST_GATEWAY_SECRET = "test-gateway-secret";
	private static final String TEST_INTERNAL_SERVICE_SECRET = "test-internal-service-secret";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void externalEndpointRejectsDirectJwtWithoutTrustedGatewayHeaders() throws Exception {
		mockMvc.perform(get("/api/v1/users/me")
				.header("Authorization", "Bearer not-a-real-token"))
				.andExpect(status().isForbidden());
	}

	@Test
	void externalEndpointRejectsSpoofedUserHeaderWithoutGatewaySecret() throws Exception {
		mockMvc.perform(get("/api/v1/users/me")
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString()))
				.andExpect(status().isForbidden());
	}

	@Test
	void externalEndpointAcceptsTrustedGatewayIdentity() throws Exception {
		mockMvc.perform(get("/api/v1/users/me")
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header(GATEWAY_SECRET_HEADER, TEST_GATEWAY_SECRET))
				.andExpect(status().isNotFound());
	}

	@Test
	void internalEndpointRejectsGatewaySecretOnly() throws Exception {
		mockMvc.perform(get("/api/v1/internal/users/{userId}", UUID.randomUUID())
				.header(GATEWAY_SECRET_HEADER, TEST_GATEWAY_SECRET))
				.andExpect(status().isForbidden());
	}

	@Test
	void internalEndpointRejectsDirectJwtOnly() throws Exception {
		mockMvc.perform(get("/api/v1/internal/users/{userId}", UUID.randomUUID())
				.header("Authorization", "Bearer not-a-real-token"))
				.andExpect(status().isForbidden());
	}

	@Test
	void internalEndpointAcceptsInternalServiceSecret() throws Exception {
		mockMvc.perform(get("/api/v1/internal/users/{userId}", UUID.randomUUID())
				.header(INTERNAL_SERVICE_SECRET_HEADER, TEST_INTERNAL_SERVICE_SECRET))
				.andExpect(status().isNotFound());
	}

	@Test
	void internalUserDirectoryIsPaginatedAndRequiresInternalSecret() throws Exception {
		mockMvc.perform(get("/api/v1/internal/users"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/internal/users")
				.header(INTERNAL_SERVICE_SECRET_HEADER, TEST_INTERNAL_SERVICE_SECRET)
				.param("page", "0")
				.param("size", "20"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/internal/users")
				.header(INTERNAL_SERVICE_SECRET_HEADER, TEST_INTERNAL_SERVICE_SECRET)
				.param("size", "201"))
				.andExpect(status().isBadRequest());
	}
}
