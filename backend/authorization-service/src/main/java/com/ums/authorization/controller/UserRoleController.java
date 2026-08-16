package com.ums.authorization.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.authorization.entity.UserRole;
import com.ums.authorization.service.UserRoleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/authorization/users")
@RequiredArgsConstructor
public class UserRoleController {

	private final UserRoleService userRoleService;

	@GetMapping("/{id}/roles")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN') or hasAuthority('ROLE_READ')")
	public List<UserRole> getUserRoles(@PathVariable UUID id) {

		return userRoleService.getUserRoles(id);
	}
}
