package com.ums.authorization.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.authorization.entity.Permission;
import com.ums.authorization.service.PermissionService;

import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

	private final PermissionService permissionService;

	@PostMapping
	public Permission create(@RequestBody Permission permission) {
		return permissionService.create(permission);
	}

	@GetMapping
	public List<Permission> getAll() {
		return permissionService.getAll();
	}

	@GetMapping("/{id}")
	public Permission getById(@PathVariable UUID id) {
		return permissionService.getById(id);
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable UUID id) {
		permissionService.delete(id);
	}

}