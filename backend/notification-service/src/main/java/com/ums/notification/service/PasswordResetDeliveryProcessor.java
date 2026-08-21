package com.ums.notification.service;

import org.springframework.stereotype.Service;

import com.ums.events.event.PasswordResetEvent;
import com.ums.notification.config.PasswordResetRetryProperties;
import com.ums.notification.publisher.PasswordResetFailurePublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetDeliveryProcessor {

	private final NotificationService notificationService;
	private final PasswordResetFailurePublisher failurePublisher;
	private final PasswordResetRetryProperties retryProperties;

	public void process(PasswordResetEvent event) {
		int maxAttempts = retryProperties.getMaxAttempts();
		RuntimeException lastFailure = null;

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				notificationService.processPasswordReset(event);
				if (attempt > 1) {
					log.info("Password-reset delivery recovered on attempt={}", attempt);
				}
				return;
			} catch (RuntimeException ex) {
				lastFailure = ex;
				log.warn(
						"Password-reset delivery attempt failed attempt={} maxAttempts={} failureType={}",
						attempt, maxAttempts, ex.getClass().getName());

				if (attempt < maxAttempts && !waitBeforeRetry(event, attempt)) {
					return;
				}
			}
		}

		publishTerminalFailure(event, lastFailure, maxAttempts);
	}

	private boolean waitBeforeRetry(PasswordResetEvent event, int failedAttempt) {
		long backoffMs = retryProperties.backoffAfterAttempt(failedAttempt);
		if (backoffMs <= 0L) {
			return true;
		}

		try {
			Thread.sleep(backoffMs);
			return true;
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.warn("Password-reset delivery retry interrupted attempt={}", failedAttempt);
			publishTerminalFailure(event, ex, failedAttempt);
			return false;
		}
	}

	private void publishTerminalFailure(PasswordResetEvent event, Throwable failure, int attempts) {
		String email = event == null ? null : event.getEmail();
		try {
			failurePublisher.publish(email, failure, attempts);
		} catch (RuntimeException publishFailure) {
			log.error(
					"Sanitized password-reset terminal failure publication failed failureType={}; sensitive source message will not be requeued",
					publishFailure.getClass().getName());
		}
	}
}
