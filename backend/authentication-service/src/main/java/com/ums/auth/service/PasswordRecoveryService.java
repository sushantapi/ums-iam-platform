package com.ums.auth.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ums.auth.dto.ForgotPasswordRequest;
import com.ums.auth.dto.ResetPasswordRequest;
import com.ums.auth.entity.PasswordResetToken;
import com.ums.auth.entity.Session;
import com.ums.auth.entity.User;
import com.ums.auth.entity.User.UserStatus;
import com.ums.auth.exception.AuthException;
import com.ums.auth.repository.PasswordResetTokenRepository;
import com.ums.auth.repository.SessionRepository;
import com.ums.auth.repository.UserRepository;
import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.AuditEvent;
import com.ums.events.event.PasswordResetEvent;
import com.ums.events.publisher.AuditPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

	private static final int RESET_TOKEN_BYTES = 32;
	private static final String LOCAL_PROVIDER = "LOCAL";
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final UserRepository userRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final SessionRepository sessionRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenBlacklistService tokenBlacklistService;
	private final JwtService jwtService;
	private final RabbitTemplate rabbitTemplate;
	private final AuditPublisher auditPublisher;

	@Value("${security.password-reset.ttl-minutes:15}")
	private long resetTokenTtlMinutes;

	@Value("${security.password-reset.reset-page-url:http://localhost:5173/reset-password}")
	private String resetPageUrl;

	@Transactional
	public void requestPasswordReset(ForgotPasswordRequest request, String ipAddress) {
		String email = normalizeEmail(request.getEmail());
		Optional<User> eligibleUser = userRepository.findByEmail(email).filter(this::isEligibleForLocalPasswordReset);

		if (eligibleUser.isPresent()) {
			issueResetToken(eligibleUser.get(), ipAddress);
		}

		publishAuditEvent(AuditEvent.builder()
				.eventType("auth.password_reset.requested")
				.serviceName("authentication-service")
				.userId(eligibleUser.map(User::getId).map(Object::toString).orElse(null))
				.userEmail(email)
				.action("PASSWORD_RESET_REQUEST")
				.entityType("USER")
				.entityId(eligibleUser.map(User::getId).map(Object::toString).orElse(null))
				.details("Password reset request accepted")
				.ipAddress(ipAddress)
				.timestamp(LocalDateTime.now())
				.build());
	}

	@Transactional
	public void resetPassword(ResetPasswordRequest request, String ipAddress) {
		Instant now = Instant.now();
		String tokenHash = DigestUtils.sha256Hex(request.getToken().trim());
		PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashForUpdate(tokenHash)
				.orElseThrow(() -> invalidResetToken(ipAddress, null));

		User user = resetToken.getUser();
		if (!resetToken.isUsableAt(now) || !isEligibleForLocalPasswordReset(user)) {
			throw invalidResetToken(ipAddress, user);
		}

		user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		user.setFailedLoginAttempts(0);
		user.setLockedUntil(null);
		resetToken.setConsumedAt(now);

		userRepository.save(user);
		passwordResetTokenRepository.save(resetToken);
		revokeSessions(user, now);

		publishAuditEvent(AuditEvent.builder()
				.eventType("auth.password_reset.completed")
				.serviceName("authentication-service")
				.userId(user.getId().toString())
				.userEmail(user.getEmail())
				.action("PASSWORD_RESET")
				.entityType("USER")
				.entityId(user.getId().toString())
				.details("Password reset completed and active sessions revoked")
				.ipAddress(ipAddress)
				.timestamp(LocalDateTime.now())
				.build());
	}

	private void issueResetToken(User user, String ipAddress) {
		Instant now = Instant.now();
		passwordResetTokenRepository.revokeActiveTokens(user.getId(), now, now);

		String rawToken = generateRawToken();
		PasswordResetToken resetToken = PasswordResetToken.builder()
				.user(user)
				.tokenHash(DigestUtils.sha256Hex(rawToken))
				.expiresAt(now.plusSeconds(Math.max(1L, resetTokenTtlMinutes) * 60L))
				.build();
		passwordResetTokenRepository.save(resetToken);

		try {
			rabbitTemplate.convertAndSend(
					RabbitMQConstants.AUTH_EXCHANGE,
					RabbitMQConstants.PASSWORD_RESET_ROUTING_KEY,
					new PasswordResetEvent(user.getEmail(), buildResetLink(rawToken)));
		} catch (Exception ex) {
			resetToken.setRevokedAt(Instant.now());
			passwordResetTokenRepository.save(resetToken);
			log.error("Password reset notification dispatch failed for userId={}", user.getId(), ex);
			publishAuditEvent(AuditEvent.builder()
					.eventType("auth.password_reset.notification_failed")
					.serviceName("authentication-service")
					.userId(user.getId().toString())
					.userEmail(user.getEmail())
					.action("PASSWORD_RESET_REQUEST")
					.entityType("USER")
					.entityId(user.getId().toString())
					.details("Password reset notification dispatch failed")
					.ipAddress(ipAddress)
					.timestamp(LocalDateTime.now())
					.build());
		}
	}

	private void revokeSessions(User user, Instant revokedAt) {
		List<Session> sessions = sessionRepository.findByUserId(user.getId());
		long ttlSeconds = Math.max(1L, (jwtService.getAccessTokenExpiryMs() + 999L) / 1000L);

		for (Session session : sessions) {
			if (!session.isRevoked()) {
				session.setRevoked(true);
				session.setRevokedAt(revokedAt);
				tokenBlacklistService.revokeSession(session.getId(), ttlSeconds);
			}
		}

		if (!sessions.isEmpty()) {
			sessionRepository.saveAll(sessions);
		}
	}

	private AuthException invalidResetToken(String ipAddress, User user) {
		publishAuditEvent(AuditEvent.builder()
				.eventType("auth.password_reset.failed")
				.serviceName("authentication-service")
				.userId(user == null ? null : user.getId().toString())
				.userEmail(user == null ? null : user.getEmail())
				.action("PASSWORD_RESET")
				.entityType("USER")
				.entityId(user == null ? null : user.getId().toString())
				.details("Invalid, expired, revoked, or consumed password reset token")
				.ipAddress(ipAddress)
				.timestamp(LocalDateTime.now())
				.build());
		return new AuthException("Invalid or expired reset token", "INVALID_PASSWORD_RESET_TOKEN");
	}

	private String generateRawToken() {
		byte[] bytes = new byte[RESET_TOKEN_BYTES];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String buildResetLink(String rawToken) {
		String separator = resetPageUrl.contains("?") ? "&" : "?";
		return resetPageUrl + separator + "token=" + rawToken;
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private boolean isEligibleForLocalPasswordReset(User user) {
		return user.getStatus() == UserStatus.ACTIVE && LOCAL_PROVIDER.equalsIgnoreCase(user.getProvider());
	}

	private void publishAuditEvent(AuditEvent event) {
		try {
			auditPublisher.publish(event);
		} catch (Exception ex) {
			log.error("Failed to publish password recovery audit event", ex);
		}
	}
}
