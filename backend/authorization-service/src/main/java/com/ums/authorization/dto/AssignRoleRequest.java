package com.ums.authorization.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignRoleRequest {

	private UUID userId;

	private String roleName;

	private String scopeType; // PLATFORM, ORG, DEPARTMENT

	private String scopeId; // *, ORG001, DEPT001

	private UUID assignedBy;
}