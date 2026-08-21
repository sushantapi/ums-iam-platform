package com.ums.auth.service;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
		prefix = "security.password-reset.audit-outbox",
		name = "enabled",
		havingValue = "true",
		matchIfMissing = true)
public class PasswordRecoveryAuditOutboxScheduler {

	private static final int BATCH_SIZE = 50;

	private final PasswordRecoveryAuditOutboxService outboxService;

	@Scheduled(
			fixedDelayString = "${security.password-reset.audit-outbox.fixed-delay-ms:5000}",
			initialDelayString = "${security.password-reset.audit-outbox.initial-delay-ms:5000}")
	public void publishPendingAuditEvents() {
		for (UUID id : outboxService.findPendingIds(BATCH_SIZE)) {
			try {
				outboxService.publishPending(id);
			} catch (Exception ex) {
				log.error("Password recovery audit outbox processing failed for id={}, errorType={}",
						id, ex.getClass().getSimpleName());
			}
		}
	}
}
