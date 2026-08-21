package com.ums.notification.consumer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.events.event.PasswordResetEvent;
import com.ums.notification.service.NotificationService;
import com.ums.notification.service.PasswordResetDeliveryProcessor;

@ExtendWith(MockitoExtension.class)
class AuthEventConsumerTests {

	@Mock
	private NotificationService notificationService;

	@Mock
	private PasswordResetDeliveryProcessor passwordResetDeliveryProcessor;

	@InjectMocks
	private AuthEventConsumer consumer;

	@Test
	void passwordResetEventsUseDedicatedBoundedRetryProcessor() {
		PasswordResetEvent event = new PasswordResetEvent(
				"user@example.com",
				"http://localhost:5174/reset-password?token=raw-sensitive-token");

		consumer.consumePasswordReset(event);

		verify(passwordResetDeliveryProcessor).process(event);
		verifyNoInteractions(notificationService);
	}
}
