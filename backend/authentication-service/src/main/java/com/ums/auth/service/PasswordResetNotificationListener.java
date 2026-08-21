package com.ums.auth.service;

import java.time.Instant;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ums.auth.repository.PasswordResetTokenRepository;
import com.ums.events.constants.RabbitMQConstants;
import com.ums.events.event.AuditEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetNotificationListener {

	private final RabbitTemplate rabbitTemplate;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final PasswordRecoveryAuditOutboxService auditOutboxService;

	@TransactionalEventListener
	public void handle(PasswordResetNotificationEvent event) {
		try {
			rabbitTemplate.convertAndSend(
					RabbitMQConstants.AUTH_EXCHANGE,
					RabbitMQConstants.PASSWORD_RESET_ROUTING_KEY,
					new com.ums.events.event.PasswordResetEvent(event.recipientEmail(), event.resetLink()));
		} catch (Exception ex) {
			revokeToken(event);
			log.error("Password reset notification dispatch failed for tokenId={}", event.tokenId(), ex);
			recordAuditFailure(event);
		}
	}

	@Transactional
	void revokeToken(PasswordResetNotificationEvent event) {
		passwordResetTokenRepository.findById(event.tokenId()).ifPresent(token -> {
			if (token.getRevokedAt() == null && token.getConsumedAt() == null) {
				token.setRevokedAt(Instant.now());
				passwordResetTokenRepository.save(token);
			}
		});
	}

	private void recordAuditFailure(PasswordResetNotificationEvent event) {
		try {
			auditOutboxService.recordInNewTransaction(AuditEvent.builder()
					.eventType("auth.password_reset.notification_failed")
					.serviceName("authentication-service")
					.userEmail(event.recipientEmail())
					.action("PASSWORD_RESET_REQUEST")
					.entityType("PASSWORD_RESET_TOKEN")
					.entityId(event.tokenId().toString())
					.details("Password reset notification dispatch failed")
					.ipAddress(event.ipAddress())
					.timestamp(java.time.LocalDateTime.now())
					.build());
		} catch (Exception auditException) {
			log.error("Failed to persist password recovery notification failure audit event; errorType={}",
					auditException.getClass().getSimpleName());
		}
	}
}
