package com.ums.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ums.auth.config.SecurityConfig;
import com.ums.auth.controller.AdminSessionController;
import com.ums.auth.controller.AuthController;
import com.ums.auth.dto.TokenResponse;
import com.ums.auth.security.InternalServiceAuthenticationFilter;
import com.ums.auth.security.TrustedGatewayAuthenticationFilter;
import com.ums.auth.service.AdminSessionService;
import com.ums.auth.service.AuthService;
import com.ums.auth.service.PasswordRecoveryService;

@WebMvcTest(
		controllers = { AuthController.class, AdminSessionController.class },
		properties = {
				"spring.cloud.config.enabled=false",
				"eureka.client.enabled=false",
				"internal.gateway.secret=test-gateway-secret",
				"internal.service.secret=test-internal-service-secret"
		})
@Import({ SecurityConfig.class, TrustedGatewayAuthenticationFilter.class, InternalServiceAuthenticationFilter.class })
class AuthenticationSecurityBoundaryTests {

	private static final String GATEWAY_SECRET_HEADER = "X-Internal-Gateway-Secret";
	private static final String INTERNAL_SERVICE_SECRET_HEADER = "X-Internal-Service-Secret";
	private static final String AUTHENTICATED_USER_HEADER = "X-Authenticated-User";
	private static final String USER_ROLES_HEADER = "X-User-Roles";
	private static final String TEST_GATEWAY_SECRET = "test-gateway-secret";
	private static final String PASSWORD_RESET_MESSAGE =
			"If an account exists for that email, password reset instructions have been sent.";

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private PasswordRecoveryService passwordRecoveryService;

	@MockitoBean
	private AdminSessionService adminSessionService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void publicAuthRoutesDoNotRequireGatewayTrust() throws Exception {
		when(authService.login(any(), any())).thenReturn(tokenResponse());
		when(authService.register(any(), any())).thenReturn(tokenResponse());
		when(authService.refreshToken(any())).thenReturn(tokenResponse());

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"user@example.com\",\"password\":\"Password@123\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"user@example.com","password":"Password@123",
						 "firstName":"Ada","lastName":"Lovelace"}
						"""))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"refresh-token\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/auth/forgot-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"user@example.com\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/auth/reset-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"opaque-reset-token\",\"newPassword\":\"NewPassword@123\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void forgotPasswordUsesIdenticalOutwardResponseForKnownAndUnknownEmails() throws Exception {
		when(passwordRecoveryService.requestPasswordReset(any(), any())).thenReturn(null);

		MvcResult knownEmail = mockMvc.perform(post("/api/v1/auth/forgot-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"user@example.com\"}"))
				.andExpect(status().isOk())
				.andReturn();

		MvcResult unknownEmail = mockMvc.perform(post("/api/v1/auth/forgot-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"missing@example.com\"}"))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(knownEmail.getResponse().getStatus())
				.isEqualTo(unknownEmail.getResponse().getStatus());

		JsonNode knownBody = objectMapper.readTree(knownEmail.getResponse().getContentAsString());
		JsonNode unknownBody = objectMapper.readTree(unknownEmail.getResponse().getContentAsString());

		assertThat(knownBody.get("success").asBoolean()).isTrue();
		assertThat(knownBody.get("message").asText()).isEqualTo(PASSWORD_RESET_MESSAGE);
		assertThat(unknownBody.get("success").asBoolean()).isTrue();
		assertThat(unknownBody.get("message").asText()).isEqualTo(PASSWORD_RESET_MESSAGE);
		assertThat(knownBody.get("data")).isNull();
		assertThat(unknownBody.get("data")).isNull();
		assertThat(knownBody.get("errorCode")).isNull();
		assertThat(unknownBody.get("errorCode")).isNull();

		// ApiResponse.timestamp is intentionally request-specific. Compare the complete
		// outward contract after removing only that non-deterministic field.
		knownBody.remove("timestamp");
		unknownBody.remove("timestamp");
		assertThat(knownBody).isEqualTo(unknownBody);
	}

	@Test
	void passwordRecoveryRoutesArePostOnlyAndAuthNamespaceIsNotBroadlyPublic() throws Exception {
		mockMvc.perform(get("/api/v1/auth/forgot-password"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/auth/reset-password"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/auth/not-a-public-route")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void logoutRejectsDirectJwtAndSpoofedGatewayIdentity() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout")
				.header("Authorization", "Bearer direct-token"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/auth/logout")
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header("Authorization", "Bearer direct-token"))
				.andExpect(status().isForbidden());
	}

	@Test
	void logoutAcceptsTrustedGatewayIdentityButNotInternalSecretAlone() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout")
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header(GATEWAY_SECRET_HEADER, TEST_GATEWAY_SECRET)
				.header("Authorization", "Bearer access-token"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/auth/logout")
				.header(INTERNAL_SERVICE_SECRET_HEADER, "test-internal-service-secret")
				.header("Authorization", "Bearer access-token"))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminSessionsRequireTrustedAdminAuthority() throws Exception {
		when(adminSessionService.listSessions(any(), any())).thenReturn(new PageImpl<>(List.of()));

		mockMvc.perform(get("/api/v1/admin/sessions")
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header(GATEWAY_SECRET_HEADER, TEST_GATEWAY_SECRET)
				.header(USER_ROLES_HEADER, "ORG_ADMIN"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/admin/sessions")
				.header(AUTHENTICATED_USER_HEADER, UUID.randomUUID().toString())
				.header(GATEWAY_SECRET_HEADER, TEST_GATEWAY_SECRET)
				.header(USER_ROLES_HEADER, "SUPER_ADMIN"))
				.andExpect(status().isOk());
	}

	@Test
	void internalBoundaryUsesOnlyInternalServiceSecret() throws Exception {
		mockMvc.perform(get("/api/v1/internal/auth/ping")
				.header(GATEWAY_SECRET_HEADER, TEST_GATEWAY_SECRET))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/internal/auth/ping")
				.header("Authorization", "Bearer direct-token"))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/internal/auth/ping")
				.header(INTERNAL_SERVICE_SECRET_HEADER, "test-internal-service-secret"))
				.andExpect(status().isNotFound());
	}

	@Test
	void swaggerIsClosed() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isForbidden());
	}

	private TokenResponse tokenResponse() {
		return TokenResponse.builder()
				.accessToken("access-token")
				.refreshToken("refresh-token")
				.tokenType("Bearer")
				.build();
	}
}
