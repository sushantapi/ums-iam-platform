package com.ums.authorization.service;

import java.util.UUID;

import com.ums.authorization.dto.AssignPermissionRequest;
import com.ums.authorization.dto.AssignRoleRequest;
import com.ums.authorization.dto.UserAuthorizationResponse;
import com.ums.authorization.dto.UserPermissionsResponse;

public interface AuthorizationService {

	String assignRole(AssignRoleRequest request);

	String assignPermission(AssignPermissionRequest request);

	UserPermissionsResponse getUserPermissions(UUID userId);

	UserPermissionsResponse getUserPermissions(UUID userId, String scopeType, String scopeId);

	boolean hasPermission(UUID userId, String permission);

	boolean hasPermission(UUID userId, String permission, String scopeType, String scopeId);

	UserAuthorizationResponse getUserAuthorization(UUID userId);

	UserAuthorizationResponse getUserAuthorization(UUID userId, String scopeType, String scopeId);

	void assignDefaultRole(UUID userId);
}