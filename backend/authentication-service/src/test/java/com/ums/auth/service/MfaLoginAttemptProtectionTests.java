package com.ums.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ums.auth.entity.MfaCredential;
import com.ums.auth.entity.MfaCredentialStatus;
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
class MfaLoginAttemptProtectionTests {

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

	private MfaService service;

	@BeforeEach
	void setUp() {
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
				new MfaProperties(),
				auditPublisher);
	}

	@Test
	void blockedUserCannotResetBudgetByStartingAnotherChallenge() {
		User user = activeMfaUser();
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(blacklistService.isMfaUserBlocked(user.getId())).thenReturn(true);

		assertThatThrownBy(() -> service.verifyLoginFactor(
				user.getId(), "123456", null, "127.0.0.1"))
				.isInstanceOf(AuthException.class)
				.extracting("errorCode")
				.isEqualTo("MFA_LOGIN_ATTEMPTS_EXCEEDED");

		verify(credentialRepository, never()).findByUserIdForUpdate(any());
	}

	@Test
	void invalidFactorAtUserBudgetLimitBlocksFurtherVerification() {
		User user = activeMfaUser();
		MfaCredential credential = activeCredential(user);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(blacklistService.isMfaUserBlocked(user.getId())).thenReturn(false);
		when(credentialRepository.findByUserIdForUpdate(user.getId())).thenReturn(Optional.of(credential));
		when(secretEncryptionService.decrypt(credential.getEncryptedSecret())).thenReturn("RAW-TOTP-SECRET");
		when(totpService.verify(any(), eq("000000"), any())).thenReturn(false);
		when(blacklistService.recordMfaUserFailure(eq(user.getId()), anyLong(), eq(5))).thenReturn(true);

		assertThatThrownBy(() -> service.verifyLoginFactor(
				user.getId(), "000000", null, "127.0.0.1"))
				.isInstanceOf(AuthException.class)
				.extracting("errorCode")
				.isEqualTo("MFA_LOGIN_ATTEMPTS_EXCEEDED");
	}

	@Test
	void successfulFactorClearsUserFailureBudget() {
		User user = activeMfaUser();
		MfaCredential credential = activeCredential(user);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(blacklistService.isMfaUserBlocked(user.getId())).thenReturn(false);
		when(credentialRepository.findByUserIdForUpdate(user.getId())).thenReturn(Optional.of(credential));
		when(secretEncryptionService.decrypt(credential.getEncryptedSecret())).thenReturn("RAW-TOTP-SECRET");
		when(totpService.verify(any(), eq("123456"), any())).thenReturn(true);

		service.verifyLoginFactor(user.getId(), "123456", null, "127.0.0.1");

		verify(blacklistService).clearMfaUserFailures(user.getId());
		verify(blacklistService, never()).recordMfaUserFailure(any(), anyLong(), anyInt());
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

	private MfaCredential activeCredential(User user) {
		return MfaCredential.builder()
				.id(UUID.randomUUID())
				.userId(user.getId())
				.encryptedSecret("v1.encrypted.secret")
				.status(MfaCredentialStatus.ACTIVE)
				.build();
	}
}
