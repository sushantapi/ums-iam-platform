package com.ums.admin.dto.request;

import jakarta.validation.constraints.Size;

public record AdminUpdateOrganizationRequest(
		@Size(min = 1, max = 255) String name,
		@Size(max = 500) String description) {
}
