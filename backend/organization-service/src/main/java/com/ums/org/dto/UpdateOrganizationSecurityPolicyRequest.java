package com.ums.org.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateOrganizationSecurityPolicyRequest(
		@NotNull(message = "requireMfa is required") Boolean requireMfa) {
}
