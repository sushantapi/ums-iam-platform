package com.ums.org.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ums.org.enums.OrganizationInvitationStatus;
import com.ums.org.enums.OrganizationRole;

public record OrganizationInvitationAcceptanceResponse(
		UUID invitationId,
		UUID organizationId,
		UUID membershipId,
		OrganizationRole role,
		OrganizationInvitationStatus status,
		LocalDateTime acceptedAt) {
}
