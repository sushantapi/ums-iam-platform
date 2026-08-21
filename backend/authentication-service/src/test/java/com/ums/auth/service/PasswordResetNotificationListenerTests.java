package com.ums.auth.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.ums.auth.entity.PasswordResetToken;
import com.ums.auth.repository.PasswordResetTokenRepository;
import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.PasswordResetEvent;
import com.ums.events.publisher.AuditPublisher;

@ExtendWith(MockitoExtension.class)
class PasswordResetNotificationListenerTests {

	@Mock private RabbitTemplate rabbitTemplate;
	@Mock private PasswordResetTokenRepository passwordResetTokenRepository;
	@Mock private AuditPublisher auditPublisher;

	@InjectMocks
	private PasswordResetNotificationListener listener;

	@Test
	void dispatchesResetLinkAfterCommitEvent() {
		UUID tokenId = UUID.randomUUID();
		PasswordResetNotificationEvent event = new PasswordResetNotificationEvent(
				tokenId, "user@example.com", "https://app.example.test/reset-password?token=opaque", "127.0.0.1");

		listener.handle(event);

		ArgumentCaptor<PasswordResetEvent> eventCaptor = ArgumentCaptor.forClass(PasswordResetEvent.class);
		verify(rabbitTemplate).convertAndSend(
				eq(RabbitMQConstants.AUTH_EXCHANGE),
				eq(RabbitMQConstants.PASSWORD_RESET_ROUTING_KEY),
				eventCaptor.capture());
		org.assertj.core.api.Assertions.assertThat(eventCaptor.getValue().getResetLink())
				.isEqualTo(event.resetLink());
	}

	@Test
	void notificationFailureRevokesTokenAndDoesNotLeakRawToken() {
		UUID tokenId = UUID.randomUUID();
		PasswordResetToken token = PasswordResetToken.builder().id(tokenId).build();
		when(passwordResetTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
		doThrow(new RuntimeException("broker unavailable"))
				.when(rabbitTemplate)
				.convertAndSend(any(String.class), any(String.class), any(Object.class));

		PasswordResetNotificationEvent event = new PasswordResetNotificationEvent(
				tokenId, "user@example.com", "https://app.example.test/reset-password?token=opaque", "127.0.0.1");

		listener.handle(event);

		verify(passwordResetTokenRepository).save(token);
		org.assertj.core.api.Assertions.assertThat(token.getRevokedAt()).isNotNull();
		verify(auditPublisher).publish(any());
	}

	@Test
	void consumedTokenIsNotRevokedWhenNotificationFails() {
		UUID tokenId = UUID.randomUUID();
		PasswordResetToken token = PasswordResetToken.builder().id(tokenId).consumedAt(java.time.Instant.now()).build();
		when(passwordResetTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
		doThrow(new RuntimeException("broker unavailable"))
				.when(rabbitTemplate)
				.convertAndSend(any(String.class), any(String.class), any(Object.class));

		listener.handle(new PasswordResetNotificationEvent(
				tokenId, "user@example.com", "https://app.example.test/reset-password?token=opaque", "127.0.0.1"));

		verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
	}
}
