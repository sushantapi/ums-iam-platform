package com.ums.notification.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.notification.message.PasswordResetDeliveryFailureMessage;

class PasswordResetFailureTopologyConfigTests {

	private final RabbitConsumerConfig consumerConfig = new RabbitConsumerConfig();
	private final PasswordResetFailureTopologyConfig failureTopologyConfig =
			new PasswordResetFailureTopologyConfig();

	@Test
	void declaresDurableTerminalFailureTopologyWithoutDeadLetteringSensitiveSourceQueue() {
		Queue sourceQueue = consumerConfig.passwordResetQueue();
		TopicExchange failureExchange = failureTopologyConfig.notificationFailureExchange();
		Queue passwordResetDlq = failureTopologyConfig.passwordResetDlq();
		Binding binding = failureTopologyConfig.passwordResetDlqBinding(passwordResetDlq, failureExchange);

		assertThat(sourceQueue.getName()).isEqualTo(RabbitMQConstants.NOTIFICATION_PASSWORD_RESET_QUEUE);
		assertThat(sourceQueue.isDurable()).isTrue();
		assertThat(sourceQueue.getArguments()).isNullOrEmpty();

		assertThat(failureExchange.getName()).isEqualTo(RabbitMQConstants.NOTIFICATION_FAILURE_EXCHANGE);
		assertThat(failureExchange.isDurable()).isTrue();
		assertThat(failureExchange.isAutoDelete()).isFalse();

		assertThat(passwordResetDlq.getName()).isEqualTo(RabbitMQConstants.NOTIFICATION_PASSWORD_RESET_DLQ);
		assertThat(passwordResetDlq.isDurable()).isTrue();

		assertThat(binding.getExchange()).isEqualTo(RabbitMQConstants.NOTIFICATION_FAILURE_EXCHANGE);
		assertThat(binding.getDestination()).isEqualTo(RabbitMQConstants.NOTIFICATION_PASSWORD_RESET_DLQ);
		assertThat(binding.getRoutingKey())
				.isEqualTo(RabbitMQConstants.NOTIFICATION_PASSWORD_RESET_FAILURE_ROUTING_KEY);
	}

	@Test
	void terminalFailureMessageSchemaContainsOnlySanitizedMetadata() {
		assertThat(Arrays.stream(PasswordResetDeliveryFailureMessage.class.getRecordComponents())
				.map(RecordComponent::getName)
				.toList())
				.containsExactly("recipientHash", "failureType", "attempts", "failedAt");

		PasswordResetDeliveryFailureMessage message = new PasswordResetDeliveryFailureMessage(
				"a".repeat(64), "MailSendException", 3, Instant.parse("2026-08-21T15:00:00Z"));

		assertThat(message.recipientHash()).hasSize(64);
		assertThat(message.failureType()).isEqualTo("MailSendException");
		assertThat(message.attempts()).isEqualTo(3);
	}

	@Test
	void terminalFailureMessageRejectsRawRecipientAndUnsafeFailureDetails() {
		Instant failedAt = Instant.parse("2026-08-21T15:00:00Z");

		assertThatThrownBy(() -> new PasswordResetDeliveryFailureMessage(
				"user@example.com", "MailSendException", 3, failedAt))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("SHA-256");

		assertThatThrownBy(() -> new PasswordResetDeliveryFailureMessage(
				"a".repeat(64), "SMTP failed for reset link http://secret", 3, failedAt))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("safe classification");
	}
}
