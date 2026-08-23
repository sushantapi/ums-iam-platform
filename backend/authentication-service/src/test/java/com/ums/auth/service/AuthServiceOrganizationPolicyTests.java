package com.ums.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ums.auth.client.AuthorizationClient;
import com.ums.auth.client.OrganizationSecurityPolicyClient;
import com.ums.auth.dto.LoginRequest;
import com.ums.auth.dto.OrganizationSecurityPolicyResponse;
import com.ums.auth.dto.UserAuthorizationResponse;
import com.ums.auth.entity.Session;
import com.ums.auth.entity.User;
import com.ums.auth.entity.User.UserStatus;
import com.ums.auth.exception.AuthException;
import com.ums.auth.exception.UmsException;
import com.ums.auth.repository.SessionRepository;
import com.ums.auth.repository.UserRepository;
import com.ums.events.publisher.AuditPublisher;

@ExtendWith(MockitoExtension.class)
class AuthServiceOrganizationPolicyTests {

	@Mock private UserRepository userRepository;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private AuditPublisher auditPublisher;
	@Mock private AuthorizationClient authorizationClient;
	@Mock private OrganizationSecurityPolicyClient organizationSecurityPolicyClient;
	@Mock private JwtService jwtService;
	@Mock private SessionRepository sessionRepository;
	@Mock private TokenBlacklistService blacklistService;
	@Mock private RabbitTemplate rabbitTemplate;
	@Mock private MfaService mfaService;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
				userRepository,
				passwordEncoder,
				auditPublisher,
				authorizationClient,
				organizationSecurityPolicyClient,
				jwtService,
				sessionRepository,
				blacklistService,
				rabbitTemplate,
				mfaService);
	}

	@Test
	void platformOnlyLoginDoesNotReadOrganizationPolicy() {
		User user = activeUser(false);
		LoginRequest request = loginRequest(null);
		stubPasswordSuccess(user, request);
		stubPlatformSession(user);

		var response = authService.login(request, "127.0.0.1");

		assertThat(response.isMfaEnrollmentRequired()).isFalse();
		assertThat(response.getRequiredOrganizationId()).isNull();
		verify(organizationSecurityPolicyClient, never()).getSecurityPolicy(any());
		verify(authorizationClient, never()).getAuthorization(user.getId(), "ORG", null);
	}

	@Test
	void disabledOrganizationMfaPolicyPreservesOrganizationScopedLogin() {
		UUID organizationId = UUID.randomUUID();
		User user = activeUser(false);
		LoginRequest request = loginRequest(organizationId);
		stubPasswordSuccess(user, request);
		when(organizationSecurityPolicyClient.getSecurityPolicy(organizationId))
				.thenReturn(new OrganizationSecurityPolicyResponse(organizationId, false, true));
		stubSessionInfrastructure(user);
		when(authorizationClient.getAuthorization(user.getId(), "ORG", organizationId.toString()))
				.thenReturn(emptyAuthorization());
		when(jwtService.generateAccessToken(any(), any(), any(), any(), any(), eq(organizationId), eq(false)))
				.thenReturn("organization-access-token");

		var response = authService.login(request, "127.0.0.1");

		assertThat(response.isMfaEnrollmentRequired()).isFalse();
		assertThat(response.getAccessToken()).isEqualTo("organization-access-token");
		ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
		verify(sessionRepository).save(sessionCaptor.capture());
		assertThat(sessionCaptor.getValue().getOrganizationId()).isEqualTo(organizationId);
		verify(authorizationClient).getAuthorization(user.getId(), "ORG", organizationId.toString());
		verify(jwtService).generateAccessToken(any(), any(), any(), any(), any(), eq(organizationId), eq(false));
	}

	@Test
	void requiredMfaWithMfaDisabledCreatesPlatformOnlyEnrollmentSession() {
		UUID organizationId = UUID.randomUUID();
		User user = activeUser(false);
		LoginRequest request = loginRequest(organizationId);
		stubPasswordSuccess(user, request);
		when(organizationSecurityPolicyClient.getSecurityPolicy(organizationId))
				.thenReturn(new OrganizationSecurityPolicyResponse(organizationId, true, true));
		stubPlatformSession(user);

		var response = authService.login(request, "127.0.0.1");

		assertThat(response.isMfaEnrollmentRequired()).isTrue();
		assertThat(response.getRequiredOrganizationId()).isEqualTo(organizationId);
		assertThat(response.getAccessToken()).isEqualTo("access-token");
		assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
		ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
		verify(sessionRepository).save(sessionCaptor.capture());
		assertThat(sessionCaptor.getValue().getOrganizationId()).isNull();
		verify(authorizationClient, never()).getAuthorization(user.getId(), "ORG", organizationId.toString());
	}

	@Test
	void requiredMfaWithMfaEnabledReturnsChallengeAndNoSession() {
		UUID organizationId = UUID.randomUUID();
		User user = activeUser(true);
		LoginRequest request = loginRequest(organizationId);
		stubPasswordSuccess(user, request);
		when(organizationSecurityPolicyClient.getSecurityPolicy(organizationId))
				.thenReturn(new OrganizationSecurityPolicyResponse(organizationId, true, true));
		when(jwtService.generateMfaChallengeToken(
				user.getId().toString(), organizationId, request.getClient(), request.getDeviceInfo()))
				.thenReturn("challenge-token");
		when(jwtService.getMfaChallengeExpiryMs()).thenReturn(300000L);

		var response = authService.login(request, "127.0.0.1");

		assertThat(response.isMfaRequired()).isTrue();
		assertThat(response.getAccessToken()).isNull();
		assertThat(response.getRefreshToken()).isNull();
		verify(sessionRepository, never()).save(any(Session.class));
	}

	@Test
	void inactiveOrganizationIsRejectedBeforeSessionCreation() {
		UUID organizationId = UUID.randomUUID();
		User user = activeUser(false);
		LoginRequest request = loginRequest(organizationId);
		stubPasswordSuccess(user, request);
		when(organizationSecurityPolicyClient.getSecurityPolicy(organizationId))
				.thenReturn(new OrganizationSecurityPolicyResponse(organizationId, false, false));

		assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
				.isInstanceOf(AuthException.class)
				.extracting("errorCode").isEqualTo("ORGANIZATION_INACTIVE");

		verify(sessionRepository, never()).save(any(Session.class));
	}

	@Test
	void policyServiceFailureFailsClosedBeforeSessionCreation() {
		UUID organizationId = UUID.randomUUID();
		User user = activeUser(false);
		LoginRequest request = loginRequest(organizationId);
		stubPasswordSuccess(user, request);
		when(organizationSecurityPolicyClient.getSecurityPolicy(organizationId))
				.thenThrow(new IllegalStateException("organization service unavailable"));

		assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
				.isInstanceOf(UmsException.class)
				.extracting("errorCode").isEqualTo("ORGANIZATION_POLICY_UNAVAILABLE");

		verify(sessionRepository, never()).save(any(Session.class));
		verify(authorizationClient, never()).getAuthorization(any(), any(), any());
	}

	private void stubPasswordSuccess(User user, LoginRequest request) {
		when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(true);
	}

	private void stubSessionInfrastructure(User user) {
		when(jwtService.getRefreshTokenExpiryMs()).thenReturn(604800000L);
		when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh-token");
		when(authorizationClient.getAuthorization(user.getId(), "PLATFORM", "*")).thenReturn(emptyAuthorization());
		when(jwtService.getAccessTokenExpiryMs()).thenReturn(900000L);
	}

	private void stubPlatformSession(User user) {
		stubSessionInfrastructure(user);
		when(jwtService.generateAccessToken(any(), any(), any(), any(), any())).thenReturn("access-token");
	}

	private UserAuthorizationResponse emptyAuthorization() {
		return UserAuthorizationResponse.builder().roles(List.of()).permissions(List.of()).build();
	}

	private LoginRequest loginRequest(UUID organizationId) {
		LoginRequest request = new LoginRequest();
		request.setEmail("user@example.com");
		request.setPassword("Password@123");
		request.setClient("ADMIN_PORTAL");
		request.setDeviceInfo("Chrome");
		request.setOrganizationId(organizationId);
		return request;
	}

	private User activeUser(boolean mfaEnabled) {
		return User.builder()
				.id(UUID.randomUUID())
				.email("user@example.com")
				.passwordHash("password-hash")
				.firstName("Ada")
				.lastName("Lovelace")
				.status(UserStatus.ACTIVE)
				.provider("LOCAL")
				.mfaEnabled(mfaEnabled)
				.build();
	}
}
