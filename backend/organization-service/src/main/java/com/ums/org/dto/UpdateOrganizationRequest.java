package com.ums.org.dto;

import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(

		@Size(min = 1, max = 255, message = "Organization name must be between 1 and 255 characters") String name,

		@Size(max = 500, message = "Description must not exceed 500 characters") String description

) {
}
