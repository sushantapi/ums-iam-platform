package com.ums.authorization.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class AssignPermissionRequest {

	private UUID roleId;

	private UUID permissionId;
}