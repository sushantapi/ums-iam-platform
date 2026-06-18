package com.ums.authorization.service;

import java.util.List;
import java.util.UUID;

import com.ums.authorization.entity.Permission;

public interface PermissionService {

	Permission create(Permission permission);

	List<Permission> getAll();

	Permission getById(UUID id);

	void delete(UUID id);
}