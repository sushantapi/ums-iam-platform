package com.ums.admin.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizationMemberResponse(UUID id, UUID userId, String role, LocalDateTime joinedAt) {
}
