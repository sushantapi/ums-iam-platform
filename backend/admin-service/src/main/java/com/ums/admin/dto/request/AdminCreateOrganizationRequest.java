package com.ums.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCreateOrganizationRequest(
		@NotBlank(message = "Organization name is required") @Size(max = 255) String name,
		@Size(max = 500) String description) {
}
