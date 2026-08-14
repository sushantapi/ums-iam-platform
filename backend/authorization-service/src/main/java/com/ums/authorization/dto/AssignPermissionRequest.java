package com.ums.authorization.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class AssignPermissionRequest {

	@NotNull
	private UUID roleId;

	@NotNull
	private UUID permissionId;
}