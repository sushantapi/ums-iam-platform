package com.ums.auth.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record AdminSessionResponse(
		UUID id,
		UUID userId,
		String userName,
		UUID organizationId,
		String organizationName,
		String device,
		String client,
		String ipAddress,
		Instant issuedAt,
		Instant lastSeenAt,
		Instant expiresAt,
		Instant revokedAt,
		String status) {
}
