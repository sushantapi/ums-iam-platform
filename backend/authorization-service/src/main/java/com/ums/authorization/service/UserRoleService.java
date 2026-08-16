package com.ums.authorization.service;

import java.util.List;
import java.util.UUID;

import com.ums.authorization.entity.UserRole;

public interface UserRoleService {

    UserRole assignRole(UserRole userRole);

    UUID revokeRoleAssignment(UUID assignmentId, UUID revokedBy);

    List<UserRole> getUserRoles(UUID userId);

    List<UserRole> getActivePlatformUserRoles(UUID userId);

    List<UserRole> getActiveUserRoles(UUID userId, String scopeType, String scopeId);
}
