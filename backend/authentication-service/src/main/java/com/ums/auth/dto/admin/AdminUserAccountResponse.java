package com.ums.auth.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record AdminUserAccountResponse(
		UUID userId,
		String email,
		String status,
		boolean locked,
		Instant lockedUntil,
		Instant lastLoginAt) {
}
