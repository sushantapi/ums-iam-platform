package com.ums.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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
import com.ums.events.event.PasswordResetEvent;
import com.ums.events.publisher.AuditPublisher;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTests {

	@Mock private UserRepository userRepository;
	@Mock private PasswordResetTokenRepository passwordResetTokenRepository;
	@Mock private SessionRepository sessionRepository;
	@Mock private PasswordEncoder passwordEncoder;
	@Mock private TokenBlacklistService tokenBlacklistService;
	@Mock private JwtService jwtService;
	@Mock private AuditPublisher auditPublisher;
	@Mock private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private PasswordRecoveryService service;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(service, "resetTokenTtlMinutes", 15L);
		ReflectionTestUtils.setField(service, "resetPageUrl", "https://app.example.test/reset-password");
	}

	@Test
	void knownEmailStoresOnlyHashAndPublishesResetEvent() {
		User user = activeLocalUser();
		ForgotPasswordRequest request = forgotRequest("USER@EXAMPLE.COM");
		when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
		when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.requestPasswordReset(request, "127.0.0.1");

		ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
		verify(passwordResetTokenRepository).save(tokenCaptor.capture());
		PasswordResetToken stored = tokenCaptor.getValue();
		assertThat(stored.getTokenHash()).hasSize(64);
		assertThat(stored.getExpiresAt()).isAfter(Instant.now());

		ArgumentCaptor<PasswordResetNotificationEvent> eventCaptor =
				ArgumentCaptor.forClass(PasswordResetNotificationEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		PasswordResetNotificationEvent event = eventCaptor.getValue();
		assertThat(event.resetLink()).startsWith("https://app.example.test/reset-password?token=");
		String rawToken = event.resetLink().substring(event.resetLink().indexOf("token=") + 6);
		assertThat(stored.getTokenHash()).isEqualTo(DigestUtils.sha256Hex(rawToken));
		assertThat(stored.getTokenHash()).doesNotContain(rawToken);
		assertThat(event.tokenId()).isEqualTo(stored.getId());
		verify(passwordResetTokenRepository).revokeActiveTokens(eq(user.getId()), any(), any());
	}

	@Test
	void unknownEmailReturnsWithoutCreatingTokenOrNotification() {
		when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

		service.requestPasswordReset(forgotRequest("missing@example.com"), "127.0.0.1");

		verify(passwordResetTokenRepository, never()).save(any());
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void validTokenChangesPasswordConsumesTokenAndRevokesSessions() {
		String rawToken = "valid-reset-token";
		User user = activeLocalUser();
		PasswordResetToken resetToken = PasswordResetToken.builder()
				.id(UUID.randomUUID())
				.user(user)
				.tokenHash(DigestUtils.sha256Hex(rawToken))
				.expiresAt(Instant.now().plusSeconds(600))
				.build();
		Session session = Session.builder()
				.id(UUID.randomUUID())
				.user(user)
				.refreshTokenHash("hash")
				.expiresAt(Instant.now().plusSeconds(3600))
				.build();

		when(passwordResetTokenRepository.findByTokenHashForUpdate(DigestUtils.sha256Hex(rawToken)))
				.thenReturn(Optional.of(resetToken));
		when(passwordEncoder.encode("NewPassword1!")).thenReturn("new-password-hash");
		when(sessionRepository.findByUserId(user.getId())).thenReturn(List.of(session));
		when(jwtService.getAccessTokenExpiryMs()).thenReturn(900000L);

		service.resetPassword(resetRequest(rawToken, "NewPassword1!"), "127.0.0.1");

		assertThat(user.getPasswordHash()).isEqualTo("new-password-hash");
		assertThat(resetToken.getConsumedAt()).isNotNull();
		assertThat(session.isRevoked()).isTrue();
		assertThat(session.getRevokedAt()).isNotNull();
		verify(tokenBlacklistService).revokeSession(session.getId(), 900L);
		verify(userRepository).save(user);
		verify(passwordResetTokenRepository).save(resetToken);
		verify(sessionRepository).saveAll(List.of(session));
	}

	@Test
	void expiredOrConsumedTokenIsRejected() {
		String rawToken = "expired-token";
		User user = activeLocalUser();
		PasswordResetToken expired = PasswordResetToken.builder()
				.user(user)
				.tokenHash(DigestUtils.sha256Hex(rawToken))
				.expiresAt(Instant.now().minusSeconds(1))
				.build();
		when(passwordResetTokenRepository.findByTokenHashForUpdate(DigestUtils.sha256Hex(rawToken)))
				.thenReturn(Optional.of(expired));

		assertThatThrownBy(() -> service.resetPassword(resetRequest(rawToken, "NewPassword1!"), "127.0.0.1"))
				.isInstanceOf(AuthException.class)
				.extracting("errorCode").isEqualTo("INVALID_PASSWORD_RESET_TOKEN");

		expired.setExpiresAt(Instant.now().plusSeconds(600));
		expired.setConsumedAt(Instant.now());
		assertThatThrownBy(() -> service.resetPassword(resetRequest(rawToken, "NewPassword1!"), "127.0.0.1"))
				.isInstanceOf(AuthException.class)
				.extracting("errorCode").isEqualTo("INVALID_PASSWORD_RESET_TOKEN");
	}

	@Test
	void unknownTokenIsRejectedWithGenericError() {
		String rawToken = "unknown-token";
		when(passwordResetTokenRepository.findByTokenHashForUpdate(DigestUtils.sha256Hex(rawToken)))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.resetPassword(resetRequest(rawToken, "NewPassword1!"), "127.0.0.1"))
				.isInstanceOf(AuthException.class)
				.hasMessage("Invalid or expired reset token")
				.extracting("errorCode").isEqualTo("INVALID_PASSWORD_RESET_TOKEN");
	}

	private User activeLocalUser() {
		return User.builder()
				.id(UUID.randomUUID())
				.email("user@example.com")
				.passwordHash("old-password-hash")
				.firstName("Ada")
				.lastName("Lovelace")
				.status(UserStatus.ACTIVE)
				.provider("LOCAL")
				.build();
	}

	private ForgotPasswordRequest forgotRequest(String email) {
		ForgotPasswordRequest request = new ForgotPasswordRequest();
		request.setEmail(email);
		return request;
	}

	private ResetPasswordRequest resetRequest(String token, String newPassword) {
		ResetPasswordRequest request = new ResetPasswordRequest();
		request.setToken(token);
		request.setNewPassword(newPassword);
		return request;
	}
}
