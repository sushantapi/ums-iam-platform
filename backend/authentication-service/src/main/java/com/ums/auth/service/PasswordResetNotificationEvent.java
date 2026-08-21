package com.ums.auth.service;

import java.util.UUID;

public record PasswordResetNotificationEvent(
		UUID tokenId,
		String recipientEmail,
		String resetLink,
		String ipAddress) {
}
