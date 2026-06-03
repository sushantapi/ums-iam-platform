package com.ums.org.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(

		@NotBlank(message = "Organization name is required") @Size(max = 255) String name,

		@Size(max = 500) String description

) {
}