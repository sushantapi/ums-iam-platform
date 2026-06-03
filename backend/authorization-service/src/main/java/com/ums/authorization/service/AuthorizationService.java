package com.ums.authorization.service;

import java.util.Set;
import java.util.UUID;

import com.ums.authorization.dto.AssignRoleRequest;
import com.ums.authorization.dto.UserAuthorizationResponse;

public interface AuthorizationService {

	String assignRole(AssignRoleRequest request);

	Set<String> getUserPermissions(UUID userId);

	boolean hasPermission(UUID userId, String permission);

	UserAuthorizationResponse getUserAuthorization(UUID userId);

	void assignDefaultRole(UUID userId);
}