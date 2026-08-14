package com.ums.authorization.dto.admin;

import java.util.UUID;

public record RoleSummaryResponse(
		UUID id, String name, String description, boolean system, boolean active) {
}
