package com.ums.notification.publisher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.ums.events.constants.RabbitMQConstants;
import com.ums.notification.message.PasswordResetDeliveryFailureMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetFailurePublisher {

	private static final String UNKNOWN_FAILURE = "UnknownFailure";

	private final RabbitTemplate rabbitTemplate;

	public void publish(String email, Throwable failure, int attempts) {
		PasswordResetDeliveryFailureMessage message = new PasswordResetDeliveryFailureMessage(
				recipientHash(email),
				failureType(failure),
				attempts,
				Instant.now());

		rabbitTemplate.convertAndSend(
				RabbitMQConstants.NOTIFICATION_FAILURE_EXCHANGE,
				RabbitMQConstants.NOTIFICATION_PASSWORD_RESET_FAILURE_ROUTING_KEY,
				message);

		log.warn(
				"Published sanitized password-reset terminal failure recipientHashPrefix={} failureType={} attempts={}",
				message.recipientHash().substring(0, 12), message.failureType(), attempts);
	}

	String recipientHash(String email) {
		String normalized = email == null ? "<blank>" : email.trim().toLowerCase(Locale.ROOT);
		if (normalized.isBlank()) {
			normalized = "<blank>";
		}

		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(normalized.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 unavailable", ex);
		}
	}

	String failureType(Throwable failure) {
		if (failure == null) {
			return UNKNOWN_FAILURE;
		}

		Throwable root = failure;
		int depth = 0;
		while (root.getCause() != null && root.getCause() != root && depth++ < 16) {
			root = root.getCause();
		}

		String type = root.getClass().getName();
		if (type.length() <= 128) {
			return type;
		}
		return type.substring(type.length() - 128);
	}
}
