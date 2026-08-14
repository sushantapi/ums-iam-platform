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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

	private final RoleService roleService;

	@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_WRITE')")
	@PostMapping
	public Role createRole(@Valid @RequestBody CreateRoleRequest request) {

		Role role = Role.builder().name(request.getName()).description(request.getDescription()).build();

		return roleService.createRole(role);
	}

	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN') or hasAuthority('ROLE_READ')")
	@GetMapping
	public List<Role> getAllRoles() {

		return roleService.getAllRoles();
	}
}
