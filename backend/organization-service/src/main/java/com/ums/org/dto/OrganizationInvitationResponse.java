package com.ums.org.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ums.org.enums.OrganizationInvitationStatus;
import com.ums.org.enums.OrganizationRole;

public record OrganizationInvitationResponse(
		UUID id,
		UUID organizationId,
		String email,
		OrganizationRole role,
		OrganizationInvitationStatus status,
		UUID inviterId,
		LocalDateTime expiresAt,
		LocalDateTime lastSentAt,
		LocalDateTime createdAt) {
}
