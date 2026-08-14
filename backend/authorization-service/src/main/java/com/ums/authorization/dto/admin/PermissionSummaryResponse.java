package com.ums.authorization.dto.admin;

import java.util.UUID;

public record PermissionSummaryResponse(
		UUID id, String code, String action, String description, boolean active) {
}
