package com.ums.authorization.service.impl;

import com.ums.authorization.entity.Permission;
import com.ums.authorization.repository.PermissionRepository;
import com.ums.authorization.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

	private final PermissionRepository permissionRepository;

	@Override
	public Permission createPermission(Permission permission) {

		return permissionRepository.save(permission);
	}

	@Override
	public List<Permission> getAllPermissions() {

		return permissionRepository.findAll();
	}
}