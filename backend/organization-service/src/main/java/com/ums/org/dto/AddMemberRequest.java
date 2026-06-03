package com.ums.org.dto;

import java.util.UUID;

import com.ums.org.enums.OrganizationRole;

import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(

		@NotNull(message = "User ID is required") UUID userId,

		@NotNull(message = "Role is required") OrganizationRole role

) {
}