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

	boolean hasPermission(UUID userId, String permission);

	UserAuthorizationResponse getUserAuthorization(UUID userId);

	void assignDefaultRole(UUID userId);
}