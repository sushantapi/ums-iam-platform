package com.ums.authorization.service;

import com.ums.authorization.entity.Permission;

import java.util.List;

public interface PermissionService {

	Permission createPermission(Permission permission);

	List<Permission> getAllPermissions();
}