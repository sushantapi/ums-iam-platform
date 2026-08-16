package com.ums.admin.dto.response;

import java.util.UUID;

public record PermissionSummaryResponse(UUID id, String code, String action, String description, boolean active) {
}
