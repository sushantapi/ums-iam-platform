package com.ums.org.dto;

import com.ums.org.enums.OrganizationRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrganizationInvitationRequest(
		@NotBlank(message = "Email is required")
		@Email(message = "Email must be valid")
		@Size(max = 255, message = "Email must not exceed 255 characters")
		String email,

		@NotNull(message = "Role is required")
		OrganizationRole role) {
}
