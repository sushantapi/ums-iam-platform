package com.ums.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ums.auth.dto.MfaRecoveryCodesResponse;
import com.ums.auth.dto.MfaSensitiveActionRequest;
import com.ums.auth.dto.MfaStatusResponse;
import com.ums.auth.dto.MfaTotpConfirmRequest;
import com.ums.auth.dto.MfaTotpSetupResponse;
import com.ums.auth.entity.MfaCredential;
import com.ums.auth.entity.MfaCredentialStatus;
import com.ums.auth.entity.MfaRecoveryCode;
import com.ums.auth.entity.Session;
import com.ums.auth.entity.User;
import com.ums.auth.entity.User.UserStatus;
import com.ums.auth.exception.AuthException;
import com.ums.auth.repository.MfaCredentialRepository;
import com.ums.auth.repository.MfaRecoveryCodeRepository;
import com.ums.auth.repository.SessionRepository;
import com.ums.auth.repository.UserRepository;
import com.ums.auth.security.mfa.MfaProperties;
import com.ums.auth.security.mfa.MfaRecoveryCodeService;
import com.ums.auth.security.mfa.MfaSecretEncryptionService;
import com.ums.auth.security.mfa.TotpService;
import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MfaService {

	private static final String LOCAL_PROVIDER = "LOCAL";
	private static final int MAX_LOGIN_FACTOR_ATTEMPTS = 5;
	private static final long LOGIN_FACTOR_ATTEMPT_WINDOW_SECONDS = 15L * 60L;

	private final UserRepository userRepository;
	private final MfaCredentialRepository credentialRepository;
	private final MfaRecoveryCodeRepository recoveryCodeRepository;
	private final SessionRepository sessionRepository;
	private final TotpService totpService;
	private final MfaSecretEncryptionService secretEncryptionService;
	private final MfaRecoveryCodeService recoveryCodeService;
	private final PasswordEncoder passwordEncoder;
	private final TokenBlacklistService blacklistService;
	private final MfaProperties properties;
	private final AuditPublisher auditPublisher;

	@Transactional
	public MfaTotpSetupResponse setupTotp(UUID userId, String ipAddress) {
		User user = requireEligibleLocalUser(userId);
		if (user.isMfaEnabled()) {
			throw new AuthException("MFA is already enabled", "MFA_ALREADY_ENABLED");
		}

		Instant now = Instant.now();
		String secret = totpService.generateSecret();
		String encryptedSecret = secretEncryptionService.encrypt(secret);
		Instant expiresAt = now.plusSeconds(properties.getTotp().getSetupExpiryMinutes() * 60L);

		MfaCredential credential = credentialRepository.findByUserIdForUpdate(userId)
				.orElseGet(() -> MfaCredential.builder().userId(userId).build());
		if (credential.getStatus() == MfaCredentialStatus.ACTIVE) {
			throw new AuthException("MFA is already enabled", "MFA_ALREADY_ENABLED");
		}
		if (credential.getId() != null) {
			recoveryCodeRepository.deleteAllByCredentialId(credential.getId());
		}

		credential.setEncryptedSecret(encryptedSecret);
		credential.setStatus(MfaCredentialStatus.PENDING);
		credential.setSetupExpiresAt(expiresAt);
		credential.setActivatedAt(null);
		credentialRepository.save(credential);

		publishAuditEvent(AuditEvent.builder()
				.eventType("auth.mfa.setup.started")
				.serviceName("authentication-service")
				.userId(user.getId().toString())
				.userEmail(user.getEmail())
				.action("MFA_SETUP")
				.entityType("USER")
				.entityId(user.getId().toString())
				.details("TOTP MFA setup started")
				.ipAddress(ipAddress)
				.timestamp(LocalDateTime.now())
				.build());

		return MfaTotpSetupResponse.builder()
				.secret(secret)
				.provisioningUri(totpService.provisioningUri(user.getEmail(), secret))
				.expiresAt(expiresAt)
				.build();
	}

	@Transactional
	public MfaRecoveryCodesResponse confirmTotp(UUID userId, MfaTotpConfirmRequest request, String ipAddress) {
		User user = requireEligibleLocalUser(userId);
		if (user.isMfaEnabled()) {
			throw new AuthException("MFA is already enabled", "MFA_ALREADY_ENABLED");
		}

		Instant now = Instant.now();
		MfaCredential credential = credentialRepository.findByUserIdForUpdate(userId)
				.orElseThrow(() -> new AuthException("No pending MFA setup", "MFA_SETUP_NOT_FOUND"));
		if (credential.getStatus() != MfaCredentialStatus.PENDING
				|| credential.getSetupExpiresAt() == null
				|| !credential.getSetupExpiresAt().isAfter(now)) {
			throw new AuthException("MFA setup has expired", "MFA_SETUP_EXPIRED");
		}

		String secret = secretEncryptionService.decrypt(credential.getEncryptedSecret());
		if (!totpService.verify(secret, request.getCode(), now)) {
			publishAuditEvent(AuditEvent.builder()
					.eventType("auth.mfa.setup.failed")
					.serviceName("authentication-service")
					.userId(user.getId().toString())
					.userEmail(user.getEmail())
					.action("MFA_CONFIRM")
					.entityType("USER")
					.entityId(user.getId().toString())
					.details("TOTP MFA confirmation failed")
					.ipAddress(ipAddress)
					.timestamp(LocalDateTime.now())
					.build());
			throw new AuthException("Invalid MFA code", "INVALID_MFA_CODE");
		}

		List<String> rawRecoveryCodes = replaceRecoveryCodes(credential);

		credential.setStatus(MfaCredentialStatus.ACTIVE);
		credential.setActivatedAt(now);
		credential.setSetupExpiresAt(null);
		credentialRepository.save(credential);

		user.setMfaEnabled(true);
		userRepository.save(user);

		publishAuditEvent(AuditEvent.builder()
				.eventType("auth.mfa.enabled")
				.serviceName("authentication-service")
				.userId(user.getId().toString())
				.userEmail(user.getEmail())
				.action("MFA_ENABLE")
				.entityType("USER")
				.entityId(user.getId().toString())
				.details("TOTP MFA enabled")
				.ipAddress(ipAddress)
				.timestamp(LocalDateTime.now())
				.build());

		return MfaRecoveryCodesResponse.builder().recoveryCodes(rawRecoveryCodes).build();
	}

	@Transactional
	public void verifyLoginFactor(UUID userId, String totpCode, String recoveryCode, String ipAddress) {
		User user = requireEligibleLocalUser(userId);
		if (blacklistService.isMfaUserBlocked(userId)) {
			publishFactorFailure(
					user,
					ipAddress,
					"auth.mfa.login.challenge.rate_limited",
					"MFA_LOGIN_VERIFY");
			throw new AuthException(
					"Too many MFA verification attempts. Try again later.",
					"MFA_LOGIN_ATTEMPTS_EXCEEDED");
		}

		MfaCredential credential = requireActiveCredential(user);
		try {
			verifyActiveFactor(
					user,
					credential,
					totpCode,
					recoveryCode,
					ipAddress,
					"auth.mfa.login.challenge.failed",
					"MFA_LOGIN_VERIFY");
		} catch (AuthException ex) {
			boolean blocked = blacklistService.recordMfaUserFailure(
					userId,
					LOGIN_FACTOR_ATTEMPT_WINDOW_SECONDS,
					MAX_LOGIN_FACTOR_ATTEMPTS);
			if (blocked) {
				throw new AuthException(
						"Too many MFA verification attempts. Try again later.",
						"MFA_LOGIN_ATTEMPTS_EXCEEDED");
			}
			throw ex;
		}

		blacklistService.clearMfaUserFailures(userId);
	}

	@Transactional
	public MfaRecoveryCodesResponse rotateRecoveryCodes(
			UUID userId,
			MfaSensitiveActionRequest request,
			String ipAddress) {
		User user = requireEligibleLocalUser(userId);
		verifyPassword(user, request.getPassword(), ipAddress, "MFA_RECOVERY_CODES_ROTATE");
		MfaCredential credential = requireActiveCredential(user);
		verifyActiveFactor(
				user,
				credential,
				request.getTotpCode(),
				request.getRecoveryCode(),
				ipAddress,
				"auth.mfa.recovery_codes.rotate.failed",
				"MFA_RECOVERY_CODES_ROTATE");

		List<String> rawRecoveryCodes = replaceRecoveryCodes(credential);
		publishAuditEvent(AuditEvent.builder()
				.eventType("auth.mfa.recovery_codes.rotated")
				.serviceName("authentication-service")
				.userId(user.getId().toString())
				.userEmail(user.getEmail())
				.action("MFA_RECOVERY_CODES_ROTATE")
				.entityType("USER")
				.entityId(user.getId().toString())
				.details("MFA recovery codes rotated")
				.ipAddress(ipAddress)
				.timestamp(LocalDateTime.now())
				.build());

		return MfaRecoveryCodesResponse.builder().recoveryCodes(rawRecoveryCodes).build();
	}

	@Transactional
	public void disableMfa(UUID userId, MfaSensitiveActionRequest request, String ipAddress) {
		User user = requireEligibleLocalUser(userId);
		verifyPassword(user, request.getPassword(), ipAddress, "MFA_DISABLE");
		MfaCredential credential = requireActiveCredential(user);
		verifyActiveFactor(
				user,
				credential,
				request.getTotpCode(),
				request.getRecoveryCode(),
				ipAddress,
				"auth.mfa.disable.failed",
				"MFA_DISABLE");

		Instant now = Instant.now();
		recoveryCodeRepository.deleteAllByCredentialId(credential.getId());
		credentialRepository.delete(credential);
		user.setMfaEnabled(false);
		userRepository.save(user);
		revokeAllUserSessions(user.getId(), now);

		publishAuditEvent(AuditEvent.builder()
				.eventType("auth.mfa.disabled")
				.serviceName("authentication-service")
				.userId(user.getId().toString())
				.userEmail(user.getEmail())
				.action("MFA_DISABLE")
				.entityType("USER")
				.entityId(user.getId().toString())
				.details("MFA disabled and active sessions revoked")
				.ipAddress(ipAddress)
				.timestamp(LocalDateTime.now())
				.build());
	}

	@Transactional(readOnly = true)
	public MfaStatusResponse status(UUID userId) {
		User user = requireEligibleLocalUser(userId);
		Instant now = Instant.now();
		MfaCredential credential = credentialRepository.findByUserId(userId).orElse(null);

		boolean enabled = user.isMfaEnabled()
				&& credential != null
				&& credential.getStatus() == MfaCredentialStatus.ACTIVE;
		boolean pending = !enabled
				&& credential != null
				&& credential.getStatus() == MfaCredentialStatus.PENDING
				&& credential.getSetupExpiresAt() != null
				&& credential.getSetupExpiresAt().isAfter(now);
		long remaining = enabled ? recoveryCodeRepository.countByCredentialIdAndConsumedAtIsNull(credential.getId()) : 0L;

		return MfaStatusResponse.builder()
				.enabled(enabled)
				.setupPending(pending)
				.setupExpiresAt(pending ? credential.getSetupExpiresAt() : null)
				.recoveryCodesRemaining(remaining)
				.build();
	}

	private User requireEligibleLocalUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new AuthException("User not found", "USER_NOT_FOUND"));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new AuthException("User is not active", "ACCOUNT_INACTIVE");
		}
		if (!LOCAL_PROVIDER.equalsIgnoreCase(user.getProvider())) {
			throw new AuthException("MFA is available only for local accounts", "MFA_UNSUPPORTED_PROVIDER");
		}
		return user;
	}

	private MfaCredential requireActiveCredential(User user) {
		if (!user.isMfaEnabled()) {
			throw new AuthException("MFA is not enabled", "MFA_NOT_ENABLED");
		}

		MfaCredential credential = credentialRepository.findByUserIdForUpdate(user.getId())
				.orElseThrow(() -> new AuthException("MFA configuration is unavailable", "MFA_CONFIGURATION_INVALID"));
		if (credential.getStatus() != MfaCredentialStatus.ACTIVE) {
			throw new AuthException("MFA configuration is unavailable", "MFA_CONFIGURATION_INVALID");
		}
		return credential;
	}

	private void verifyPassword(User user, String password, String ipAddress, String action) {
		if (StringUtils.hasText(password) && passwordEncoder.matches(password, user.getPasswordHash())) {
			return;
		}

		publishFactorFailure(user, ipAddress, "auth.mfa.sensitive_action.failed", action);
		throw new AuthException("Invalid credentials", "INVALID_CREDENTIALS");
	}

	private void verifyActiveFactor(
			User user,
			MfaCredential credential,
			String totpCode,
			String recoveryCode,
			String ipAddress,
			String failureEventType,
			String action) {
		boolean hasTotp = StringUtils.hasText(totpCode);
		boolean hasRecoveryCode = StringUtils.hasText(recoveryCode);
		if (hasTotp == hasRecoveryCode) {
			publishFactorFailure(user, ipAddress, failureEventType, action);
			throw new AuthException("Provide exactly one MFA factor", "MFA_FACTOR_REQUIRED");
		}

		Instant now = Instant.now();
		if (hasTotp) {
			String secret = secretEncryptionService.decrypt(credential.getEncryptedSecret());
			if (!totpService.verify(secret, totpCode, now)) {
				publishFactorFailure(user, ipAddress, failureEventType, action);
				throw new AuthException("Invalid MFA code", "INVALID_MFA_CODE");
			}
			return;
		}

		MfaRecoveryCode matchedCode = null;
		for (MfaRecoveryCode candidate : recoveryCodeRepository
				.findAllByCredentialIdAndConsumedAtIsNull(credential.getId())) {
			if (recoveryCodeService.matches(recoveryCode, candidate.getCodeHash())) {
				matchedCode = candidate;
			}
		}

		if (matchedCode == null) {
			publishFactorFailure(user, ipAddress, failureEventType, action);
			throw new AuthException("Invalid MFA recovery code", "INVALID_MFA_RECOVERY_CODE");
		}

		matchedCode.setConsumedAt(now);
		recoveryCodeRepository.save(matchedCode);
		publishAuditEvent(AuditEvent.builder()
				.eventType("auth.mfa.recovery_code.used")
				.serviceName("authentication-service")
				.userId(user.getId().toString())
				.userEmail(user.getEmail())
				.action("MFA_RECOVERY_CODE_USE")
				.entityType("USER")
				.entityId(user.getId().toString())
				.details("MFA recovery code consumed")
				.ipAddress(ipAddress)
				.timestamp(LocalDateTime.now())
				.build());
	}

	private List<String> replaceRecoveryCodes(MfaCredential credential) {
		List<String> rawRecoveryCodes = recoveryCodeService.generateCodes();
		recoveryCodeRepository.deleteAllByCredentialId(credential.getId());
		List<MfaRecoveryCode> storedCodes = rawRecoveryCodes.stream()
				.map(rawCode -> MfaRecoveryCode.builder()
						.credentialId(credential.getId())
						.codeHash(recoveryCodeService.hash(rawCode))
						.build())
				.toList();
		recoveryCodeRepository.saveAll(storedCodes);
		return rawRecoveryCodes;
	}

	private void revokeAllUserSessions(UUID userId, Instant now) {
		List<Session> sessions = sessionRepository.findByUserId(userId);
		for (Session session : sessions) {
			session.setRevoked(true);
			if (session.getRevokedAt() == null) {
				session.setRevokedAt(now);
			}

			if (session.getExpiresAt() != null) {
				long ttlSeconds = Duration.between(now, session.getExpiresAt()).getSeconds();
				if (ttlSeconds > 0) {
					blacklistService.revokeSession(session.getId(), ttlSeconds);
				}
			}
		}
		if (!sessions.isEmpty()) {
			sessionRepository.saveAll(sessions);
		}
	}

	private void publishFactorFailure(User user, String ipAddress, String eventType, String action) {
		publishAuditEvent(AuditEvent.builder()
				.eventType(eventType)
				.serviceName("authentication-service")
				.userId(user.getId().toString())
				.userEmail(user.getEmail())
				.action(action)
				.entityType("USER")
				.entityId(user.getId().toString())
				.details("MFA verification failed")
				.ipAddress(ipAddress)
				.timestamp(LocalDateTime.now())
				.build());
	}

	private void publishAuditEvent(AuditEvent event) {
		try {
			auditPublisher.publish(event);
		} catch (Exception ex) {
			log.error("Failed to publish MFA audit event", ex);
		}
	}
}
