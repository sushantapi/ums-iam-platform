package com.ums.auth.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ums.auth.client.AuthorizationClient;
import com.ums.auth.dto.LoginRequest;
import com.ums.auth.dto.RefreshTokenRequest;
import com.ums.auth.dto.RegisterRequest;
import com.ums.auth.dto.TokenResponse;
import com.ums.auth.dto.UserAuthorizationResponse;
import com.ums.auth.entity.Session;
import com.ums.auth.entity.User;
import com.ums.auth.entity.User.UserStatus;
import com.ums.auth.exception.AuthException;
import com.ums.auth.exception.UmsException;
import com.ums.auth.repository.SessionRepository;
import com.ums.auth.repository.UserRepository;
import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.AuditEvent;
import com.ums.events.event.user.UserRegisteredEvent;
import com.ums.events.publisher.AuditPublisher;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private static final int MAX_FAILED_ATTEMPTS = 5;
	private static final int LOCKOUT_MINUTES = 30;

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	private final AuditPublisher auditPublisher;

	private final AuthorizationClient authorizationClient;

	private final JwtService jwtService;

	private final SessionRepository sessionRepository;
	private final TokenBlacklistService blacklistService;

	private final RabbitTemplate rabbitTemplate;

	@Transactional
	public TokenResponse register(RegisterRequest request, String ipAddress) {

		String email = request.getEmail().trim().toLowerCase();

		if (userRepository.existsByEmail(email)) {
			throw new AuthException("Email already registered", "EMAIL_EXISTS");
		}

		User user = User.builder().email(email).passwordHash(passwordEncoder.encode(request.getPassword()))
				.firstName(request.getFirstName().trim()).lastName(request.getLastName().trim())
				.status(UserStatus.ACTIVE).provider(request.getProvider() != null ? request.getProvider() : "LOCAL")
				.externalId(request.getExternalId()).build();


		User savedUser = userRepository.save(user);

		authorizationClient.assignDefaultRole(savedUser.getId());

		UserRegisteredEvent event = UserRegisteredEvent.builder().userId(savedUser.getId()).email(savedUser.getEmail())
				.firstName(savedUser.getFirstName()).lastName(savedUser.getLastName()).build();

		rabbitTemplate.convertAndSend(RabbitMQConstants.USER_EXCHANGE, RabbitMQConstants.USER_REGISTERED_ROUTING_KEY,
				event);

		publishAuditEvent(AuditEvent.builder().eventType("auth.registration.completed")
				.serviceName("authentication-service").userId(savedUser.getId().toString())
				.userEmail(savedUser.getEmail()).action("REGISTER").entityType("USER")
				.entityId(savedUser.getId().toString()).details("User registered successfully").ipAddress(ipAddress)
				.timestamp(LocalDateTime.now()).build());

		Session session = Session.builder().user(savedUser).refreshTokenHash("pending").ipAddress(ipAddress)
				.lastSeenAt(Instant.now())
				.expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiryMs())).build();
		String refreshToken = jwtService.generateRefreshToken(savedUser.getId().toString(), session.getId());
		session.setRefreshTokenHash(hash(refreshToken));
		sessionRepository.save(session);

		log.info("User registered successfully: {}", savedUser.getEmail());

		return buildTokenResponse(savedUser, refreshToken, session.getId());
	}

	@Transactional(noRollbackFor = AuthException.class)
	public TokenResponse login(LoginRequest request, String ipAddress) {

		String email = request.getEmail().trim().toLowerCase();

		User user = userRepository.findByEmail(email).orElseThrow(() -> {

			publishAuditEvent(AuditEvent.builder().eventType("auth.login.failed").serviceName("authentication-service")
					.userEmail(email).action("LOGIN").entityType("USER").details("Email not found").ipAddress(ipAddress)
					.timestamp(LocalDateTime.now()).build());

			return new AuthException("Invalid credentials", "INVALID_CREDENTIALS");
		});

		if (user.isLocked()) {
			throw new AuthException("Account is locked. Please try again later.", "ACCOUNT_LOCKED");
		}

		if (user.getStatus() == UserStatus.SUSPENDED) {
			throw new AuthException("Account suspended. Contact support.", "ACCOUNT_SUSPENDED");
		}

		if (user.getStatus() == UserStatus.DELETED) {
			throw new AuthException("Invalid credentials", "INVALID_CREDENTIALS");
		}

		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {

			handleFailedLogin(user, ipAddress);

			throw new AuthException("Invalid credentials", "INVALID_CREDENTIALS");
		}

		user.setFailedLoginAttempts(0);
		user.setLockedUntil(null);
		user.setLastLoginAt(Instant.now());

		Session session = Session.builder().user(user).refreshTokenHash("pending").ipAddress(ipAddress)
				.deviceInfo(request.getDeviceInfo())
				.client(request.getClient())
				.organizationId(request.getOrganizationId())
				.lastSeenAt(Instant.now())
				.expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiryMs())).build();
		String refreshToken = jwtService.generateRefreshToken(user.getId().toString(), session.getId());
		session.setRefreshTokenHash(hash(refreshToken));

		sessionRepository.save(session);
		userRepository.save(user);

		publishAuditEvent(AuditEvent.builder().eventType("auth.login.succeeded").serviceName("authentication-service")
				.userId(user.getId().toString()).userEmail(user.getEmail()).action("LOGIN").entityType("USER")
				.entityId(user.getId().toString()).details("User logged in successfully").ipAddress(ipAddress)
				.timestamp(LocalDateTime.now()).build());

		log.info("Login successful: {}", email);

		return buildTokenResponse(user, refreshToken, session.getId());
	}

	private String hash(String token) {

		return DigestUtils.sha256Hex(token);
	}

	private void handleFailedLogin(User user, String ipAddress) {

		int attempts = user.getFailedLoginAttempts() + 1;

		user.setFailedLoginAttempts(attempts);

		if (attempts >= MAX_FAILED_ATTEMPTS) {

			user.setLockedUntil(Instant.now().plusSeconds(LOCKOUT_MINUTES * 60L));

			log.warn("User account locked: {}", user.getEmail());
		}

		userRepository.save(user);

		publishAuditEvent(AuditEvent.builder().eventType("auth.login.failed").serviceName("authentication-service")
				.userId(user.getId().toString()).userEmail(user.getEmail()).action("LOGIN").entityType("USER")
				.entityId(user.getId().toString()).details("Invalid password").ipAddress(ipAddress)
				.timestamp(LocalDateTime.now()).build());
	}

	/*
	 * private TokenResponse buildTokenResponse(User user, String refreshToken) {
	 * 
	 * Set<String> roles =
	 * user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
	 * 
	 * String accessToken = jwtService.generateAccessToken(user.getId().toString(),
	 * user.getEmail(), roles);
	 * 
	 * return
	 * TokenResponse.builder().accessToken(accessToken).refreshToken(refreshToken).
	 * tokenType("Bearer") .expiresIn(jwtService.getAccessTokenExpiryMs() /
	 * 1000).userId(user.getId().toString()) .email(user.getEmail()).build(); }
	 */

	private TokenResponse buildTokenResponse(User user, String refreshToken, UUID sessionId) {

		UserAuthorizationResponse authorization;

		try {
			authorization = authorizationClient.getAuthorization(user.getId());
		} catch (Exception ex) {
			log.error("Authorization service unavailable while issuing token for user {}", user.getId(), ex);
			throw new UmsException(
					"Authorization service is unavailable",
					ex,
					HttpStatus.SERVICE_UNAVAILABLE,
					"AUTHORIZATION_UNAVAILABLE");
		}

		Set<String> roles = new HashSet<>(authorization.getRoles());

		Set<String> permissions = new HashSet<>(authorization.getPermissions());

		String accessToken = jwtService.generateAccessToken(user.getId().toString(), user.getEmail(), roles,
				permissions, sessionId);

		return TokenResponse.builder().accessToken(accessToken).refreshToken(refreshToken).tokenType("Bearer")
				.expiresIn(jwtService.getAccessTokenExpiryMs() / 1000).userId(user.getId().toString())
				.email(user.getEmail()).build();
	}

	@Transactional
	public TokenResponse refreshToken(RefreshTokenRequest request) {

		Claims claims;
		try {
			claims = jwtService.validateAndExtract(request.getRefreshToken());
		} catch (Exception ex) {
			throw new AuthException("Invalid refresh token", "INVALID_REFRESH_TOKEN");
		}

		if (!"REFRESH".equals(claims.get("type", String.class))
				|| !StringUtils.hasText(claims.getId())
				|| !StringUtils.hasText(claims.getSubject())) {

			throw new AuthException("Invalid refresh token", "INVALID_REFRESH_TOKEN");
		}

		UUID sessionId = parseSessionId(claims);
		Session session = sessionRepository.findByIdForRefresh(sessionId)
				.orElseThrow(() -> new AuthException("Invalid refresh token", "INVALID_REFRESH_TOKEN"));

		if (!secureEquals(session.getRefreshTokenHash(), hash(request.getRefreshToken()))) {
			throw new AuthException("Refresh token has already been rotated", "REFRESH_TOKEN_REPLAYED");
		}

		if (session.isRevoked()) {
			throw new AuthException("Session revoked", "SESSION_REVOKED");
		}
		if (session.getExpiresAt() == null || !session.getExpiresAt().isAfter(Instant.now())) {
			throw new AuthException("Session expired", "SESSION_EXPIRED");
		}

		User user = session.getUser();
		if (!user.getId().toString().equals(claims.getSubject()) || user.getStatus() != UserStatus.ACTIVE) {
			throw new AuthException("User is not active", "ACCOUNT_INACTIVE");
		}

		String newRefreshToken = jwtService.generateRefreshToken(user.getId().toString(), session.getId());

		session.setRefreshTokenHash(hash(newRefreshToken));

		session.setLastSeenAt(Instant.now());

		session.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiryMs()));

		sessionRepository.save(session);

		return buildTokenResponse(user, newRefreshToken, session.getId());
	}

//	@Transactional
//	public void logout(HttpServletRequest request) {
//
//		String token = request.getHeader("Authorization").replace("Bearer ", "");
//
//		Claims claims = jwtService.validateAndExtract(token);
//
//		String jti = claims.getId();
//
//		long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
//
//		blacklistService.blacklist(jti, ttl / 1000);
//
//		auditService.log(null, "LOGOUT", request.getRemoteAddr(), "SUCCESS");
//	}

	@Transactional
	public void logout(HttpServletRequest request, UUID trustedUserId) {

		try {

			String token = extractToken(request);

			Claims claims = jwtService.validateAndExtract(token);
			if (!"ACCESS".equals(claims.get("type", String.class))
					|| !StringUtils.hasText(claims.getId())
					|| !trustedUserId.toString().equals(claims.getSubject())) {
				throw new AuthException("Invalid access token", "INVALID_ACCESS_TOKEN");
			}

			String jti = claims.getId();
			UUID sessionId = parseSessionId(claims);
			Session session = sessionRepository.findById(sessionId)
					.orElseThrow(() -> new AuthException("Session not found", "SESSION_NOT_FOUND"));
			if (!session.getUser().getId().equals(trustedUserId)) {
				throw new AuthException("Invalid access token", "INVALID_ACCESS_TOKEN");
			}

			long ttl = claims.getExpiration().getTime() / 1000 - System.currentTimeMillis() / 1000;

			if (ttl > 0) {
				blacklistService.blacklist(jti, ttl);
				blacklistService.revokeSession(sessionId, ttl);
			}
			session.setRevoked(true);
			session.setRevokedAt(Instant.now());
			sessionRepository.save(session);

			publishAuditEvent(AuditEvent.builder().eventType("auth.logout.completed")
					.serviceName("authentication-service").userId(claims.getSubject()).action("LOGOUT")
					.entityType("SESSION").entityId(sessionId.toString()).details("User logged out successfully")
					.ipAddress(request.getRemoteAddr()).timestamp(LocalDateTime.now()).build());

			log.info("User logged out successfully. UserId={}", claims.getSubject());

		} catch (AuthException ex) {
			throw ex;
		} catch (Exception ex) {

			log.error("Logout failed", ex);

			throw new AuthException("Logout failed", "LOGOUT_FAILED");
		}
	}

	private UUID parseSessionId(Claims claims) {
		String sessionId = claims.get("sessionId", String.class);
		if (!StringUtils.hasText(sessionId)) {
			throw new AuthException("Token is not bound to a session", "INVALID_TOKEN_SESSION");
		}
		try {
			return UUID.fromString(sessionId);
		} catch (IllegalArgumentException ex) {
			throw new AuthException("Token contains an invalid session", "INVALID_TOKEN_SESSION");
		}
	}

	private boolean secureEquals(String storedHash, String suppliedHash) {
		if (storedHash == null || suppliedHash == null) {
			return false;
		}
		return MessageDigest.isEqual(
				storedHash.getBytes(StandardCharsets.UTF_8),
				suppliedHash.getBytes(StandardCharsets.UTF_8));
	}

	private void publishAuditEvent(AuditEvent event) {

		try {
			auditPublisher.publish(event);
		} catch (Exception ex) {
			log.error("Failed to publish audit event", ex);
		}
	}

	private String extractToken(HttpServletRequest request) {

		String authHeader = request.getHeader("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			throw new RuntimeException("Authorization token missing");
		}

		return authHeader.substring(7);
	}
}
