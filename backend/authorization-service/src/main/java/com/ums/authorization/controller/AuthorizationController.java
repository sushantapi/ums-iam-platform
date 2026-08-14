package com.ums.authorization.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ums.authorization.dto.AssignPermissionRequest;
import com.ums.authorization.dto.AssignRoleRequest;
import com.ums.authorization.dto.PermissionCheckResponse;
import com.ums.authorization.dto.UserPermissionsResponse;
import com.ums.authorization.service.AuthorizationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/authorization")
@RequiredArgsConstructor
public class AuthorizationController {

	private final AuthorizationService authorizationService;

	@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_WRITE')")
	@PostMapping("/assign-role")
	public String assignRole(@Valid @RequestBody AssignRoleRequest request) {

		return authorizationService.assignRole(request);
	}

	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN') or hasAuthority('ROLE_READ')")
	@GetMapping("/users/{id}/permissions")
	public UserPermissionsResponse getUserPermissions(
			@PathVariable UUID id,
			@RequestParam(defaultValue = "PLATFORM") String scopeType,
			@RequestParam(defaultValue = "*") String scopeId) {

		return authorizationService.getUserPermissions(id, scopeType, scopeId);
	}

	@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_WRITE')")
	@PostMapping("/assign-permission")
	public String assignPermission(@Valid @RequestBody AssignPermissionRequest request) {

		return authorizationService.assignPermission(request);
	}

	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN') or hasAuthority('ROLE_READ')")
	@GetMapping("/check")
	public PermissionCheckResponse checkPermission(

			@RequestParam UUID userId,

			@RequestParam String permission,
			@RequestParam(defaultValue = "PLATFORM") String scopeType,
			@RequestParam(defaultValue = "*") String scopeId) {

		boolean allowed = authorizationService.hasPermission(userId, permission, scopeType, scopeId);

		return PermissionCheckResponse.builder()

				.allowed(allowed)

				.build();
	}

}
