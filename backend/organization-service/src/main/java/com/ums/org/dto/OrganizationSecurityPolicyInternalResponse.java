package com.ums.org.dto;

import java.util.UUID;

public record OrganizationSecurityPolicyInternalResponse(
		UUID organizationId,
		boolean requireMfa,
		boolean active) {
}
