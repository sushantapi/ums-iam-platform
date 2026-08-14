package com.ums.authorization.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignRoleRequest {

	@NotNull
	private UUID userId;

	@NotBlank
	@Pattern(regexp = "^[A-Z][A-Z0-9_]{1,63}$")
	private String roleName;

	@Pattern(regexp = "^(PLATFORM|ORG|DEPARTMENT)$")
	private String scopeType; // PLATFORM, ORG, DEPARTMENT

	@Size(max = 36)
	private String scopeId; // *, organization UUID/string, department identifier

	private UUID assignedBy;
}