package com.ums.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ums.auth.client.AuthorizationClient;
import com.ums.auth.client.OrganizationSecurityPolicyClient;
import com.ums.auth.dto.LoginRequest;
import com.ums.auth.dto.MfaChallengeVerifyRequest;
import com.ums.auth.dto.OrganizationSecurityPolicyResponse;
import com.ums.auth.dto.UserAuthorizationResponse;
import com.ums.auth.entity.Session;
import com.ums.auth.entity.User;
import com.ums.auth.entity.User.UserStatus;
import com.ums.auth.exception.AuthException;
import com.ums.auth.repository.SessionRepository;
import com.ums.auth.repository.UserRepository;
import com.ums.events.publisher.AuditPublisher;

import io.jsonwebtoken.Claims;

@ExtendWith(MockitoExtension.class)
class AuthServiceMfaLoginTests {

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

	@InjectMocks
	private AuthService authService;

	@Test
	void passwordLoginWithMfaEnabledIssuesOnlyChallengeAndNoSessionOrTokens() {
		User user = activeMfaUser();
		LoginRequest request = loginRequest();
		when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(true);
		when(organizationSecurityPolicyClient.getSecurityPolicy(request.getOrganizationId()))
				.thenReturn(new OrganizationSecurityPolicyResponse(request.getOrganizationId(), true, true));
		when(jwtService.generateMfaChallengeToken(
				user.getId().toString(), request.getOrganizationId(), request.getClient(), request.getDeviceInfo()))
				.thenReturn("signed-mfa-challenge");
		when(jwtService.getMfaChallengeExpiryMs()).thenReturn(300000L);

		var response = authService.login(request, "127.0.0.1");

		assertThat(response.isMfaRequired()).isTrue();
		assertThat(response.getMfaChallengeToken()).isEqualTo("signed-mfa-challenge");
		assertThat(response.getMfaChallengeExpiresIn()).isEqualTo(300L);
		assertThat(response.getAccessToken()).isNull();
		assertThat(response.getRefreshToken()).isNull();
		verify(sessionRepository, never()).save(any(Session.class));
		verify(jwtService, never()).generateAccessToken(any(), any(), any(), any(), any());
		verify(jwtService, never()).generateRefreshToken(any(), any());
	}

	@Test
	void validTotpChallengeCreatesSessionAndReturnsNormalTokens() {
		User user = activeMfaUser();
		UUID organizationId = UUID.randomUUID();
		Claims claims = challengeClaims(user.getId(), organizationId, "challenge-jti");
		MfaChallengeVerifyRequest request = challengeRequest("signed-mfa-challenge", "123456", null);

		when(jwtService.validateAndExtract("signed-mfa-challenge")).thenReturn(claims);
		when(blacklistService.isMfaChallengeConsumed("challenge-jti")).thenReturn(false);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(organizationSecurityPolicyClient.getSecurityPolicy(organizationId))
				.thenReturn(new OrganizationSecurityPolicyResponse(organizationId, true, true));
		when(blacklistService.consumeMfaChallenge(eq("challenge-jti"), anyLong())).thenReturn(true);
		when(jwtService.getRefreshTokenExpiryMs()).thenReturn(604800000L);
		when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh-token");
		when(authorizationClient.getAuthorization(user.getId(), "PLATFORM", "*"))
				.thenReturn(UserAuthorizationResponse.builder().roles(List.of()).permissions(List.of()).build());
		when(authorizationClient.getAuthorization(user.getId(), "ORG", organizationId.toString()))
				.thenReturn(UserAuthorizationResponse.builder().roles(List.of()).permissions(List.of()).build());
		when(jwtService.generateAccessToken(any(), any(), any(), any(), any())).thenReturn("access-token");
		when(jwtService.getAccessTokenExpiryMs()).thenReturn(900000L);

		var response = authService.verifyMfaChallenge(request, "127.0.0.1");

		assertThat(response.isMfaRequired()).isFalse();
		assertThat(response.getAccessToken()).isEqualTo("access-token");
		assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
		verify(mfaService).verifyLoginFactor(user.getId(), "123456", null, "127.0.0.1");
		ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
		verify(sessionRepository).save(sessionCaptor.capture());
		assertThat(sessionCaptor.getValue().isMfaVerified()).isTrue();
	}

	@Test
	void consumedChallengeIsRejectedBeforeSecondFactorOrSessionCreation() {
		User user = activeMfaUser();
		Claims claims = challengeClaims(user.getId(), null, "replayed-jti");
		MfaChallengeVerifyRequest request = challengeRequest("replayed-challenge", "123456", null);
		when(jwtService.validateAndExtract("replayed-challenge")).thenReturn(claims);
		when(blacklistService.isMfaChallengeConsumed("replayed-jti")).thenReturn(true);

		assertThatThrownBy(() -> authService.verifyMfaChallenge(request, "127.0.0.1"))
				.isInstanceOf(AuthException.class)
				.extracting("errorCode").isEqualTo("MFA_CHALLENGE_REPLAYED");

		verify(mfaService, never()).verifyLoginFactor(any(), any(), any(), any());
		verify(sessionRepository, never()).save(any(Session.class));
	}

	@Test
	void invalidOrTamperedChallengeIsRejectedBeforeUserLookup() {
		MfaChallengeVerifyRequest request = challengeRequest("tampered-challenge", "123456", null);
		when(jwtService.validateAndExtract("tampered-challenge"))
				.thenThrow(new IllegalArgumentException("invalid signature"));

		assertThatThrownBy(() -> authService.verifyMfaChallenge(request, "127.0.0.1"))
				.isInstanceOf(AuthException.class)
				.extracting("errorCode").isEqualTo("INVALID_MFA_CHALLENGE");

		verify(userRepository, never()).findById(any());
		verify(sessionRepository, never()).save(any(Session.class));
	}

	@Test
	void repeatedInvalidFactorsConsumeChallengeAtAttemptLimit() {
		User user = activeMfaUser();
		Claims claims = challengeClaims(user.getId(), null, "limited-jti");
		MfaChallengeVerifyRequest request = challengeRequest("limited-challenge", "000000", null);
		when(jwtService.validateAndExtract("limited-challenge")).thenReturn(claims);
		when(blacklistService.isMfaChallengeConsumed("limited-jti")).thenReturn(false);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		doThrow(new AuthException("Invalid MFA code", "INVALID_MFA_CODE"))
				.when(mfaService).verifyLoginFactor(user.getId(), "000000", null, "127.0.0.1");
		when(blacklistService.recordMfaChallengeFailure(eq("limited-jti"), anyLong(), eq(5))).thenReturn(true);
		when(blacklistService.consumeMfaChallenge(eq("limited-jti"), anyLong())).thenReturn(true);

		assertThatThrownBy(() -> authService.verifyMfaChallenge(request, "127.0.0.1"))
				.isInstanceOf(AuthException.class)
				.extracting("errorCode").isEqualTo("MFA_CHALLENGE_ATTEMPTS_EXCEEDED");

		verify(blacklistService).consumeMfaChallenge(eq("limited-jti"), anyLong());
		verify(sessionRepository, never()).save(any(Session.class));
	}

	private User activeMfaUser() {
		return User.builder()
				.id(UUID.randomUUID())
				.email("user@example.com")
				.passwordHash("password-hash")
				.firstName("Ada")
				.lastName("Lovelace")
				.status(UserStatus.ACTIVE)
				.provider("LOCAL")
				.mfaEnabled(true)
				.build();
	}

	private LoginRequest loginRequest() {
		LoginRequest request = new LoginRequest();
		request.setEmail("user@example.com");
		request.setPassword("Password@123");
		request.setClient("ADMIN_PORTAL");
		request.setDeviceInfo("Chrome");
		request.setOrganizationId(UUID.randomUUID());
		return request;
	}

	private MfaChallengeVerifyRequest challengeRequest(String token, String totpCode, String recoveryCode) {
		MfaChallengeVerifyRequest request = new MfaChallengeVerifyRequest();
		request.setChallengeToken(token);
		request.setTotpCode(totpCode);
		request.setRecoveryCode(recoveryCode);
		return request;
	}

	private Claims challengeClaims(UUID userId, UUID organizationId, String jti) {
		Claims claims = mock(Claims.class);
		lenient().when(claims.get("type", String.class)).thenReturn("MFA_CHALLENGE");
		lenient().when(claims.getId()).thenReturn(jti);
		lenient().when(claims.getSubject()).thenReturn(userId.toString());
		lenient().when(claims.getExpiration()).thenReturn(Date.from(Instant.now().plusSeconds(300)));
		lenient().when(claims.get("organizationId", String.class))
				.thenReturn(organizationId == null ? null : organizationId.toString());
		lenient().when(claims.get("client", String.class)).thenReturn("ADMIN_PORTAL");
		lenient().when(claims.get("deviceInfo", String.class)).thenReturn("Chrome");
		return claims;
	}
}
