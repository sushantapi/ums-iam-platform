package com.ums.notification.message;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

public record PasswordResetDeliveryFailureMessage(
		String recipientHash,
		String failureType,
		int attempts,
		Instant failedAt) {

	private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-f]{64}");
	private static final Pattern SAFE_FAILURE_TYPE = Pattern.compile("[A-Za-z0-9.$_-]{1,128}");

	public PasswordResetDeliveryFailureMessage {
		if (recipientHash == null || !SHA256_HEX.matcher(recipientHash).matches()) {
			throw new IllegalArgumentException("recipientHash must be a lowercase SHA-256 hex digest");
		}

		if (failureType == null || !SAFE_FAILURE_TYPE.matcher(failureType).matches()) {
			throw new IllegalArgumentException("failureType must contain only safe classification characters");
		}

		if (attempts < 1) {
			throw new IllegalArgumentException("attempts must be at least 1");
		}

		Objects.requireNonNull(failedAt, "failedAt is required");
	}
}
