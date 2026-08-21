package com.ums.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.events.event.PasswordResetEvent;
import com.ums.notification.config.PasswordResetRetryProperties;
import com.ums.notification.publisher.PasswordResetFailurePublisher;

@ExtendWith(MockitoExtension.class)
class PasswordResetDeliveryProcessorTests {

	@Mock
	private NotificationService notificationService;

	@Mock
	private PasswordResetFailurePublisher failurePublisher;

	private PasswordResetRetryProperties retryProperties;
	private PasswordResetDeliveryProcessor processor;

	@BeforeEach
	void setUp() {
		retryProperties = new PasswordResetRetryProperties();
		retryProperties.setMaxAttempts(3);
		retryProperties.setInitialBackoffMs(0L);
		retryProperties.setMaxBackoffMs(0L);
		retryProperties.setMultiplier(2.0d);
		processor = new PasswordResetDeliveryProcessor(notificationService, failurePublisher, retryProperties);
	}

	@Test
	void successfulFirstAttemptDoesNotPublishTerminalFailure() {
		PasswordResetEvent event = event();

		processor.process(event);

		verify(notificationService).processPasswordReset(event);
		verifyNoInteractions(failurePublisher);
	}

	@Test
	void transientFailureRetriesAndStopsAfterRecovery() {
		PasswordResetEvent event = event();
		IllegalStateException firstFailure = new IllegalStateException("transient");
		doThrow(firstFailure).doNothing().when(notificationService).processPasswordReset(event);

		processor.process(event);

		verify(notificationService, times(2)).processPasswordReset(event);
		verifyNoInteractions(failurePublisher);
	}

	@Test
	void exhaustedRetriesPublishExactlyOneTerminalFailure() {
		PasswordResetEvent event = event();
		IllegalStateException deliveryFailure = new IllegalStateException("delivery failed");
		doThrow(deliveryFailure).when(notificationService).processPasswordReset(event);

		processor.process(event);

		verify(notificationService, times(3)).processPasswordReset(event);
		verify(failurePublisher).publish(eq(event.getEmail()), same(deliveryFailure), eq(3));
	}

	@Test
	void terminalPublisherFailureDoesNotRequeueSensitiveSourceMessage() {
		PasswordResetEvent event = event();
		IllegalStateException deliveryFailure = new IllegalStateException("delivery failed");
		doThrow(deliveryFailure).when(notificationService).processPasswordReset(event);
		doThrow(new IllegalStateException("terminal publisher unavailable"))
				.when(failurePublisher).publish(eq(event.getEmail()), same(deliveryFailure), eq(3));

		assertThatCode(() -> processor.process(event)).doesNotThrowAnyException();

		verify(notificationService, times(3)).processPasswordReset(event);
		verify(failurePublisher).publish(eq(event.getEmail()), same(deliveryFailure), eq(3));
	}

	@Test
	void configuredBackoffIsBoundedAndStopsAfterFinalAttempt() {
		retryProperties.setMaxAttempts(4);
		retryProperties.setInitialBackoffMs(100L);
		retryProperties.setMultiplier(2.0d);
		retryProperties.setMaxBackoffMs(250L);

		assertThat(retryProperties.backoffAfterAttempt(1)).isEqualTo(100L);
		assertThat(retryProperties.backoffAfterAttempt(2)).isEqualTo(200L);
		assertThat(retryProperties.backoffAfterAttempt(3)).isEqualTo(250L);
		assertThat(retryProperties.backoffAfterAttempt(4)).isZero();
	}

	private PasswordResetEvent event() {
		return new PasswordResetEvent(
				"user@example.com",
				"http://localhost:5174/reset-password?token=raw-sensitive-token");
	}
}
