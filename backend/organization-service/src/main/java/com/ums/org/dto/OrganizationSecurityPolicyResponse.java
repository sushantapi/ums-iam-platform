package com.ums.org.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizationSecurityPolicyResponse(
		UUID organizationId,
		boolean requireMfa,
		LocalDateTime updatedAt) {
}
