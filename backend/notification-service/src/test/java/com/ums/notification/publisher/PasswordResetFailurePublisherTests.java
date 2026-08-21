package com.ums.notification.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mail.MailSendException;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.notification.message.PasswordResetDeliveryFailureMessage;

@ExtendWith(MockitoExtension.class)
class PasswordResetFailurePublisherTests {

	@Mock
	private RabbitTemplate rabbitTemplate;

	@Test
	void publishesOnlySanitizedTerminalFailureMetadata() throws Exception {
		PasswordResetFailurePublisher publisher = new PasswordResetFailurePublisher(rabbitTemplate);
		String email = "User@Example.com";
		String secret = "raw-sensitive-token";
		MailSendException mailFailure = new MailSendException(
				"SMTP failed for http://localhost/reset-password?token=" + secret);
		IllegalStateException wrapper = new IllegalStateException("delivery failed", mailFailure);

		publisher.publish(email, wrapper, 3);

		ArgumentCaptor<PasswordResetDeliveryFailureMessage> captor =
				ArgumentCaptor.forClass(PasswordResetDeliveryFailureMessage.class);
		verify(rabbitTemplate).convertAndSend(
				eq(RabbitMQConstants.NOTIFICATION_FAILURE_EXCHANGE),
				eq(RabbitMQConstants.NOTIFICATION_PASSWORD_RESET_FAILURE_ROUTING_KEY),
				captor.capture());

		PasswordResetDeliveryFailureMessage message = captor.getValue();
		String expectedHash = HexFormat.of().formatHex(
				MessageDigest.getInstance("SHA-256")
						.digest("user@example.com".getBytes(StandardCharsets.UTF_8)));

		assertThat(message.recipientHash()).isEqualTo(expectedHash);
		assertThat(message.failureType()).isEqualTo(MailSendException.class.getName());
		assertThat(message.attempts()).isEqualTo(3);
		assertThat(message.toString())
				.doesNotContain(email)
				.doesNotContain(secret)
				.doesNotContain("reset-password?token=");
	}
}
