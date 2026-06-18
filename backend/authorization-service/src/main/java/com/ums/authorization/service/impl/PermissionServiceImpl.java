package com.ums.authorization.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ums.authorization.entity.Permission;
import com.ums.authorization.exception.PermissionNotFoundException;
import com.ums.authorization.repository.PermissionRepository;
import com.ums.authorization.service.PermissionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

	private final PermissionRepository repository;

	@Override
	public Permission create(Permission permission) {
		return repository.save(permission);
	}

	@Override
	public List<Permission> getAll() {
		return repository.findAll();
	}

	@Override
	public Permission getById(UUID id) {
		return repository.findById(id).orElseThrow(() -> new PermissionNotFoundException("Permission not found"));
	}

	@Override
	public void delete(UUID id) {
		repository.deleteById(id);
	}

}