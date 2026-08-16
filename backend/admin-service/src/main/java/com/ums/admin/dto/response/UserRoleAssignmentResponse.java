package com.ums.admin.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserRoleAssignmentResponse(
		UUID assignmentId, UUID roleId, String roleName, String scopeType, String scopeId,
		boolean active, LocalDateTime assignedAt, LocalDateTime expiresAt) {
}
