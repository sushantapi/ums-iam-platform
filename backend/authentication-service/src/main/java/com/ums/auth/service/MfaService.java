package com.ums.auth.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ums.auth.dto.MfaRecoveryCodesResponse;
import com.ums.auth.dto.MfaStatusResponse;
import com.ums.auth.dto.MfaTotpConfirmRequest;
import com.ums.auth.dto.MfaTotpSetupResponse;
import com.ums.auth.entity.MfaCredential;
import com.ums.auth.entity.MfaCredentialStatus;
import com.ums.auth.entity.MfaRecoveryCode;
import com.ums.auth.entity.User;
import com.ums.auth.entity.User.UserStatus;
import com.ums.auth.exception.AuthException;
import com.ums.auth.repository.MfaCredentialRepository;
import com.ums.auth.repository.MfaRecoveryCodeRepository;
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

	private final UserRepository userRepository;
	private final MfaCredentialRepository credentialRepository;
	private final MfaRecoveryCodeRepository recoveryCodeRepository;
	private final TotpService totpService;
	private final MfaSecretEncryptionService secretEncryptionService;
	private final MfaRecoveryCodeService recoveryCodeService;
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

		List<String> rawRecoveryCodes = recoveryCodeService.generateCodes();
		recoveryCodeRepository.deleteAllByCredentialId(credential.getId());
		List<MfaRecoveryCode> storedCodes = rawRecoveryCodes.stream()
				.map(rawCode -> MfaRecoveryCode.builder()
						.credentialId(credential.getId())
						.codeHash(recoveryCodeService.hash(rawCode))
						.build())
				.toList();
		recoveryCodeRepository.saveAll(storedCodes);

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

	private void publishAuditEvent(AuditEvent event) {
		try {
			auditPublisher.publish(event);
		} catch (Exception ex) {
			log.error("Failed to publish MFA audit event", ex);
		}
	}
}
