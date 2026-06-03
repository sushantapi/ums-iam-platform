package com.ums.authorization.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AssignRoleRequest {

	private UUID userId;

	private String roleName;
}