package com.ums.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ums.auth.client.AuthorizationClient;
import com.ums.auth.dto.RefreshTokenRequest;
import com.ums.auth.dto.UserAuthorizationResponse;
import com.ums.auth.entity.Session;
import com.ums.auth.entity.User;
import com.ums.auth.entity.User.UserStatus;
import com.ums.auth.exception.AuthException;
import com.ums.auth.exception.UmsException;
import com.ums.auth.repository.SessionRepository;
import com.ums.auth.repository.UserRepository;
import com.ums.events.publisher.AuditPublisher;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshTests {

	@Mock private UserRepository userRepository;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private AuditPublisher auditPublisher;
	@Mock private AuthorizationClient authorizationClient;
	@Mock private JwtService jwtService;
	@Mock private SessionRepository sessionRepository;
	@Mock private TokenBlacklistService blacklistService;
	@Mock private RabbitTemplate rabbitTemplate;

	@InjectMocks
	private AuthService authService;

	@Test
	void refreshRotatesStoredHashAndReturnsNewTokens() {
		UUID userId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		String oldToken = "old-refresh-token";
		String newToken = "new-refresh-token";
		User user = activeUser(userId);
		Session session = activeSession(sessionId, user, oldToken);
		RefreshTokenRequest request = request(oldToken);

		when(jwtService.validateAndExtract(oldToken)).thenReturn(claims("REFRESH", userId, sessionId));
		when(sessionRepository.findByIdForRefresh(sessionId)).thenReturn(Optional.of(session));
		when(jwtService.generateRefreshToken(userId.toString(), sessionId)).thenReturn(newToken);
		when(jwtService.getRefreshTokenExpiryMs()).thenReturn(604800000L);
		when(authorizationClient.getAuthorization(userId))
				.thenReturn(UserAuthorizationResponse.builder().roles(List.of()).permissions(List.of()).build());
		when(jwtService.generateAccessToken(any(), any(), any(), any(), any())).thenReturn("new-access-token");

		var response = authService.refreshToken(request);

		assertThat(response.getRefreshToken()).isEqualTo(newToken);
		assertThat(session.getRefreshTokenHash()).isEqualTo(DigestUtils.sha256Hex(newToken));
		assertThat(session.getLastSeenAt()).isNotNull();
		verify(sessionRepository).save(session);
	}

	@Test
	void refreshFailsClosedWhenAuthorizationServiceIsUnavailable() {
		UUID userId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		String oldToken = "old-refresh-token";
		String newToken = "new-refresh-token";
		User user = activeUser(userId);
		Session session = activeSession(sessionId, user, oldToken);

		when(jwtService.validateAndExtract(oldToken)).thenReturn(claims("REFRESH", userId, sessionId));
		when(sessionRepository.findByIdForRefresh(sessionId)).thenReturn(Optional.of(session));
		when(jwtService.generateRefreshToken(userId.toString(), sessionId)).thenReturn(newToken);
		when(jwtService.getRefreshTokenExpiryMs()).thenReturn(604800000L);
		when(authorizationClient.getAuthorization(userId)).thenThrow(new IllegalStateException("down"));

		assertThatThrownBy(() -> authService.refreshToken(request(oldToken)))
				.isInstanceOf(UmsException.class)
				.extracting("errorCode").isEqualTo("AUTHORIZATION_UNAVAILABLE");
	}

	@Test
	void refreshRejectsAccessTokenAndRevokedSession() {
		UUID userId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		RefreshTokenRequest request = request("token");

		when(jwtService.validateAndExtract("token")).thenReturn(claims("ACCESS", userId, sessionId));
		assertThatThrownBy(() -> authService.refreshToken(request))
				.isInstanceOf(AuthException.class)
				.extracting("errorCode").isEqualTo("INVALID_REFRESH_TOKEN");

		Session revoked = activeSession(sessionId, activeUser(userId), "token");
		revoked.setRevoked(true);
		when(jwtService.validateAndExtract("token")).thenReturn(claims("REFRESH", userId, sessionId));
		when(sessionRepository.findByIdForRefresh(sessionId)).thenReturn(Optional.of(revoked));
		assertThatThrownBy(() -> authService.refreshToken(request))
				.isInstanceOf(AuthException.class)
				.extracting("errorCode").isEqualTo("SESSION_REVOKED");
	}

	@Test
	void replayedOldRefreshTokenIsRejectedAfterRotation() {
		UUID userId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		Session session = activeSession(sessionId, activeUser(userId), "current-token");
		when(jwtService.validateAndExtract("old-token")).thenReturn(claims("REFRESH", userId, sessionId));
		when(sessionRepository.findByIdForRefresh(sessionId)).thenReturn(Optional.of(session));

		assertThatThrownBy(() -> authService.refreshToken(request("old-token")))
				.isInstanceOf(AuthException.class)
				.extracting("errorCode").isEqualTo("REFRESH_TOKEN_REPLAYED");
	}

	@Test
	void logoutRejectsRefreshTokensEvenBehindTrustedGatewayIdentity() {
		UUID userId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Authorization")).thenReturn("Bearer refresh-token");
		when(jwtService.validateAndExtract("refresh-token")).thenReturn(claims("REFRESH", userId, sessionId));

		assertThatThrownBy(() -> authService.logout(request, userId))
				.isInstanceOf(AuthException.class)
				.extracting("errorCode").isEqualTo("INVALID_ACCESS_TOKEN");
	}

	private Claims claims(String type, UUID userId, UUID sessionId) {
		return Jwts.claims()
				.subject(userId.toString())
				.id(UUID.randomUUID().toString())
				.add("type", type)
				.add("sessionId", sessionId.toString())
				.build();
	}

	private RefreshTokenRequest request(String token) {
		RefreshTokenRequest request = new RefreshTokenRequest();
		request.setRefreshToken(token);
		return request;
	}

	private User activeUser(UUID userId) {
		return User.builder()
				.id(userId)
				.email("user@example.com")
				.passwordHash("hash")
				.firstName("Ada")
				.lastName("Lovelace")
				.status(UserStatus.ACTIVE)
				.build();
	}

	private Session activeSession(UUID sessionId, User user, String rawToken) {
		return Session.builder()
				.id(sessionId)
				.user(user)
				.refreshTokenHash(DigestUtils.sha256Hex(rawToken))
				.expiresAt(Instant.now().plusSeconds(3600))
				.build();
	}
}
