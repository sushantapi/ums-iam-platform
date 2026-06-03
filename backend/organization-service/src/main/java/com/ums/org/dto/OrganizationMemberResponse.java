package com.ums.org.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ums.org.enums.OrganizationRole;

public record OrganizationMemberResponse(

		UUID id, UUID userId, OrganizationRole role, LocalDateTime joinedAt

) {
}