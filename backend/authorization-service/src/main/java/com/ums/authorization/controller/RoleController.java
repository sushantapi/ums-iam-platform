package com.ums.authorization.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.authorization.dto.CreateRoleRequest;
import com.ums.authorization.entity.Role;
import com.ums.authorization.service.RoleService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

	private final RoleService roleService;

	// ADMIN ONLY

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping
	public Role createRole(@RequestBody CreateRoleRequest request) {

		Role role = Role.builder().name(request.getName()).description(request.getDescription()).build();

		return roleService.createRole(role);
	}

	// ADMIN + USER

	@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
	@GetMapping
	public List<Role> getAllRoles() {

		return roleService.getAllRoles();
	}
}