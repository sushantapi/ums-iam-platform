package com.ums.authorization.controller;

import java.util.Set;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ums.authorization.dto.AssignRoleRequest;
import com.ums.authorization.dto.PermissionCheckResponse;
import com.ums.authorization.service.AuthorizationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/authorization")
@RequiredArgsConstructor
public class AuthorizationController {

	private final AuthorizationService authorizationService;

	@PostMapping("/assign-role")
	public String assignRole(@RequestBody AssignRoleRequest request) {

		return authorizationService.assignRole(request);
	}

	@GetMapping("/users/{id}/permissions")
	public Set<String> getUserPermissions(@PathVariable UUID id) {

		return authorizationService.getUserPermissions(id);
	}

	@GetMapping("/check")
	public PermissionCheckResponse checkPermission(

			@RequestParam UUID userId,

			@RequestParam String permission) {

		boolean allowed = authorizationService.hasPermission(userId, permission);

		return PermissionCheckResponse.builder()

				.allowed(allowed)

				.build();
	}

}