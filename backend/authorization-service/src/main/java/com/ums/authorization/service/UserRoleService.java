package com.ums.authorization.service;

import java.util.List;
import java.util.UUID;

import com.ums.authorization.entity.UserRole;

public interface UserRoleService {

	UserRole assignRole(UserRole userRole);

	List<UserRole> getUserRoles(UUID userId);
}