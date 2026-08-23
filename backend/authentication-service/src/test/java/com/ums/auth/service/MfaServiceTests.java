package com.ums.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ums.auth.dto.MfaRecoveryCodesResponse;
import com.ums.auth.dto.MfaSensitiveActionRequest;
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
import com.ums.events.publisher.AuditPublisher;

@ExtendWith(MockitoExtension.class)
class MfaServiceTests {

	@Mock private UserRepository userRepository;
	@Mock private MfaCredentialRepository credentialRepository;
	@Mock private MfaRecoveryCodeRepository recoveryCodeRepository;
	@Mock private SessionRepository sessionRepository;
	@Mock private TotpService totpService;
	@Mock private MfaSecretEncryptionService secretEncryptionService;
	@Mock private MfaRecoveryCodeService recoveryCodeService;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private TokenBlacklistService blacklistService;
	@Mock private AuditPublisher auditPublisher;

	private MfaProperties properties;
	private MfaService service;

	@BeforeEach
	void setUp() {
		properties = new MfaProperties();
		service = new MfaService(
				userRepository,
				credentialRepository,
				recoveryCodeRepository,
				sessionRepository,
				totpService,
				secretEncryptionService,
				recoveryCodeService,
				passwordEncoder,
				blacklistService,
				properties,
				auditPublisher);
	}

	@Test
	void setupDoesNotEnableMfaAndPersistsOnlyEncryptedSecret() {
		User user = activeLocalUser();
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(credentialRepository.findByUserIdForUpdate(user.getId())).thenReturn(Optional.empty());
		when(totpService.generateSecret()).thenReturn("RAW-TOTP-SECRET");
		when(secretEncryptionService.encrypt("RAW-TOTP-SECRET")).thenReturn("v1.encrypted.secret");
		when(totpService.provisioningUri(user.getEmail(), "RAW-TOTP-SECRET")).thenReturn("otpauth://safe-uri");
		when(credentialRepository.save(any(MfaCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));

		MfaTotpSetupResponse response = service.setupTotp(user.getId(), "127.0.0.1");

		ArgumentCaptor<MfaCredential> credentialCaptor = ArgumentCaptor.forClass(MfaCredential.class);
		verify(credentialRepository).save(credentialCaptor.capture());
		MfaCredential stored = credentialCaptor.getValue();
		assertThat(stored.getEncryptedSecret()).isEqualTo("v1.encrypted.secret");
		assertThat(stored.getEncryptedSecret()).doesNotContain("RAW-TOTP-SECRET");
		assertThat(stored.getStatus()).isEqualTo(MfaCredentialStatus.PENDING);
		assertThat(stored.getSetupExpiresAt()).isAfter(Instant.now());
		assertThat(user.isMfaEnabled()).isFalse();
		assertThat(response.getSecret()).isEqualTo("RAW-TOTP-SECRET");
		assertThat(response.getProvisioningUri()).isEqualTo("otpauth://safe-uri");
		verify(userRepository, never()).save(user);
	}

	@Test
	void confirmEnablesMfaAndStoresOnlyRecoveryCodeHashes() {
		User user = activeLocalUser();
		UUID credentialId = UUID.randomUUID();
		MfaCredential credential = MfaCredential.builder()
				.id(credentialId)
				.userId(user.getId())
				.encryptedSecret("v1.encrypted.secret")
				.status(MfaCredentialStatus.PENDING)
				.setupExpiresAt(Instant.now().plusSeconds(300))
				.build();
		MfaTotpConfirmRequest request = new MfaTotpConfirmRequest();
		request.setCode("123456");

		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(credentialRepository.findByUserIdForUpdate(user.getId())).thenReturn(Optional.of(credential));
		when(secretEncryptionService.decrypt("v1.encrypted.secret")).thenReturn("RAW-TOTP-SECRET");
		when(totpService.verify(any(), any(), any())).thenReturn(true);
		when(recoveryCodeService.generateCodes()).thenReturn(List.of("AAAA-BBBB-CCCC-DDDD", "EEEE-FFFF-GGGG-HHHH"));
		when(recoveryCodeService.hash("AAAA-BBBB-CCCC-DDDD")).thenReturn("a".repeat(64));
		when(recoveryCodeService.hash("EEEE-FFFF-GGGG-HHHH")).thenReturn("b".repeat(64));

		MfaRecoveryCodesResponse response = service.confirmTotp(user.getId(), request, "127.0.0.1");

		assertThat(user.isMfaEnabled()).isTrue();
		assertThat(credential.getStatus()).isEqualTo(MfaCredentialStatus.ACTIVE);
		assertThat(credential.getActivatedAt()).isNotNull();
		assertThat(credential.getSetupExpiresAt()).isNull();
		assertThat(response.getRecoveryCodes()).containsExactly("AAAA-BBBB-CCCC-DDDD", "EEEE-FFFF-GGGG-HHHH");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<MfaRecoveryCode>> codesCaptor = ArgumentCaptor.forClass(List.class);
		verify(recoveryCodeRepository).saveAll(codesCaptor.capture());
		assertThat(codesCaptor.getValue()).allSatisfy(code -> {
			assertThat(code.getCredentialId()).isEqualTo(credentialId);
			assertThat(code.getCodeHash()).hasSize(64);
			assertThat(code.getCodeHash()).doesNotContain("AAAA-BBBB");
			assertThat(code.getCodeHash()).doesNotContain("EEEE-FFFF");
		});
		verify(userRepository).save(user);
		verify(credentialRepository).save(credential);
	}

	@Test
	void invalidTotpDoesNotEnableMfaOrCreateRecoveryCodes() {
		User user = activeLocalUser();
		MfaCredential credential = MfaCredential.builder()
				.id(UUID.randomUUID())
				.userId(user.getId())
				.encryptedSecret("v1.encrypted.secret")
				.status(MfaCredentialStatus.PENDING)
				.setupExpiresAt(Instant.now().plusSeconds(300))
				.build();
		MfaTotpConfirmRequest request = new MfaTotpConfirmRequest();
		request.setCode("000000");

		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(credentialRepository.findByUserIdForUpdate(user.getId())).thenReturn(Optional.of(credential));
		when(secretEncryptionService.decrypt("v1.encrypted.secret")).thenReturn("RAW-TOTP-SECRET");
		when(totpService.verify(any(), any(), any())).thenReturn(false);

		assertThatThrownBy(() -> service.confirmTotp(user.getId(), request, "127.0.0.1"))
				.isInstanceOf(AuthException.class)
				.extracting("errorCode").isEqualTo("INVALID_MFA_CODE");

		assertThat(user.isMfaEnabled()).isFalse();
		assertThat(credential.getStatus()).isEqualTo(MfaCredentialStatus.PENDING);
		verify(recoveryCodeRepository, never()).saveAll(any());
		verify(userRepository, never()).save(any());
	}

	@Test
	void recoveryCodeIsConsumedOnceAndReplayFails() {
		User user = activeLocalUser();
		user.setMfaEnabled(true);
		UUID credentialId = UUID.randomUUID();
		MfaCredential credential = activeCredential(user, credentialId);
		MfaRecoveryCode recoveryCode = MfaRecoveryCode.builder()
				.id(UUID.randomUUID())
				.credentialId(credentialId)
				.codeHash("a".repeat(64))
				.build();

		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(credentialRepository.findByUserIdForUpdate(user.getId())).thenReturn(Optional.of(credential));
		when(recoveryCodeRepository.findAllByCredentialIdAndConsumedAtIsNull(credentialId))
				.thenReturn(List.of(recoveryCode), List.of());
		when(recoveryCodeService.matches("AAAA-BBBB-CCCC-DDDD", recoveryCode.getCodeHash())).thenReturn(true);

		service.verifyLoginFactor(user.getId(), null, "AAAA-BBBB-CCCC-DDDD", "127.0.0.1");

		assertThat(recoveryCode.getConsumedAt()).isNotNull();
		verify(recoveryCodeRepository).save(recoveryCode);

		assertThatThrownBy(() -> service.verifyLoginFactor(
				user.getId(), null, "AAAA-BBBB-CCCC-DDDD", "127.0.0.1"))
				.isInstanceOf(AuthException.class)
				.extracting("errorCode").isEqualTo("INVALID_MFA_RECOVERY_CODE");
	}

	@Test
	void rotateRecoveryCodesRequiresPasswordAndFactorAndReplacesOldCodes() {
		User user = activeLocalUser();
		user.setMfaEnabled(true);
		UUID credentialId = UUID.randomUUID();
		MfaCredential credential = activeCredential(user, credentialId);
		MfaSensitiveActionRequest request = sensitiveRequest("Password@123", "123456", null);

		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Password@123", user.getPasswordHash())).thenReturn(true);
		when(credentialRepository.findByUserIdForUpdate(user.getId())).thenReturn(Optional.of(credential));
		when(secretEncryptionService.decrypt(credential.getEncryptedSecret())).thenReturn("RAW-TOTP-SECRET");
		when(totpService.verify(any(), eq("123456"), any())).thenReturn(true);
		when(recoveryCodeService.generateCodes()).thenReturn(List.of("NEW1-AAAA-BBBB-CCCC", "NEW2-DDDD-EEEE-FFFF"));
		when(recoveryCodeService.hash("NEW1-AAAA-BBBB-CCCC")).thenReturn("c".repeat(64));
		when(recoveryCodeService.hash("NEW2-DDDD-EEEE-FFFF")).thenReturn("d".repeat(64));

		MfaRecoveryCodesResponse response = service.rotateRecoveryCodes(user.getId(), request, "127.0.0.1");

		assertThat(response.getRecoveryCodes()).containsExactly("NEW1-AAAA-BBBB-CCCC", "NEW2-DDDD-EEEE-FFFF");
		verify(recoveryCodeRepository).deleteAllByCredentialId(credentialId);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<MfaRecoveryCode>> codesCaptor = ArgumentCaptor.forClass(List.class);
		verify(recoveryCodeRepository).saveAll(codesCaptor.capture());
		assertThat(codesCaptor.getValue()).extracting(MfaRecoveryCode::getCodeHash)
				.containsExactly("c".repeat(64), "d".repeat(64));
	}

	@Test
	void disableMfaDeletesCredentialAndRevokesEveryActiveSession() {
		User user = activeLocalUser();
		user.setMfaEnabled(true);
		UUID credentialId = UUID.randomUUID();
		MfaCredential credential = activeCredential(user, credentialId);
		MfaSensitiveActionRequest request = sensitiveRequest("Password@123", "123456", null);
		Instant now = Instant.now();
		Session first = session(user, now.plusSeconds(3600));
		Session second = session(user, now.plusSeconds(7200));
		List<Session> sessions = List.of(first, second);

		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Password@123", user.getPasswordHash())).thenReturn(true);
		when(credentialRepository.findByUserIdForUpdate(user.getId())).thenReturn(Optional.of(credential));
		when(secretEncryptionService.decrypt(credential.getEncryptedSecret())).thenReturn("RAW-TOTP-SECRET");
		when(totpService.verify(any(), eq("123456"), any())).thenReturn(true);
		when(sessionRepository.findByUserId(user.getId())).thenReturn(sessions);

		service.disableMfa(user.getId(), request, "127.0.0.1");

		assertThat(user.isMfaEnabled()).isFalse();
		assertThat(sessions).allSatisfy(session -> {
			assertThat(session.isRevoked()).isTrue();
			assertThat(session.getRevokedAt()).isNotNull();
		});
		verify(recoveryCodeRepository).deleteAllByCredentialId(credentialId);
		verify(credentialRepository).delete(credential);
		verify(userRepository).save(user);
		verify(sessionRepository).saveAll(sessions);
		verify(blacklistService).revokeSession(eq(first.getId()), anyLong());
		verify(blacklistService).revokeSession(eq(second.getId()), anyLong());
	}

	@Test
	void sensitiveMfaActionRejectsWrongPasswordBeforeFactorOrSessionMutation() {
		User user = activeLocalUser();
		user.setMfaEnabled(true);
		MfaSensitiveActionRequest request = sensitiveRequest("wrong-password", "123456", null);

		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong-password", user.getPasswordHash())).thenReturn(false);

		assertThatThrownBy(() -> service.disableMfa(user.getId(), request, "127.0.0.1"))
				.isInstanceOf(AuthException.class)
				.extracting("errorCode").isEqualTo("INVALID_CREDENTIALS");

		verify(credentialRepository, never()).findByUserIdForUpdate(any());
		verify(sessionRepository, never()).findByUserId(any());
		verify(blacklistService, never()).revokeSession(any(), anyLong());
	}

	private MfaCredential activeCredential(User user, UUID credentialId) {
		return MfaCredential.builder()
				.id(credentialId)
				.userId(user.getId())
				.encryptedSecret("v1.encrypted.secret")
				.status(MfaCredentialStatus.ACTIVE)
				.build();
	}

	private MfaSensitiveActionRequest sensitiveRequest(String password, String totpCode, String recoveryCode) {
		MfaSensitiveActionRequest request = new MfaSensitiveActionRequest();
		request.setPassword(password);
		request.setTotpCode(totpCode);
		request.setRecoveryCode(recoveryCode);
		return request;
	}

	private Session session(User user, Instant expiresAt) {
		return Session.builder()
				.id(UUID.randomUUID())
				.user(user)
				.refreshTokenHash("hash")
				.expiresAt(expiresAt)
				.build();
	}

	private User activeLocalUser() {
		return User.builder()
				.id(UUID.randomUUID())
				.email("user@example.com")
				.passwordHash("password-hash")
				.firstName("Ada")
				.lastName("Lovelace")
				.status(UserStatus.ACTIVE)
				.provider("LOCAL")
				.build();
	}
}
