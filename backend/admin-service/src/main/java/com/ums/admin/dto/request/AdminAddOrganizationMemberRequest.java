package com.ums.admin.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AdminAddOrganizationMemberRequest(
		@NotNull UUID userId,
		@NotNull @Pattern(regexp = "^(ADMIN|MEMBER)$") String role) {
}
