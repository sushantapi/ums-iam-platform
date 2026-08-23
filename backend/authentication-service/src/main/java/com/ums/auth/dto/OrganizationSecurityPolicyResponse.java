package com.ums.auth.dto;

import java.util.UUID;

public record OrganizationSecurityPolicyResponse(
		UUID organizationId,
		boolean requireMfa,
		boolean active) {
}
