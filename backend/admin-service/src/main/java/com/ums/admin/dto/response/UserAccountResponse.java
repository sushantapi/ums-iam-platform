package com.ums.admin.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserAccountResponse(
		UUID userId, String email, String status, boolean locked, Instant lockedUntil, Instant lastLoginAt) {
}
