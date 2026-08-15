package com.ums.admin.controller;

import java.util.Set;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ums.admin.dto.response.GrantPageResponse;
import com.ums.admin.service.AdminRoleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/grants")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','AUTH_ADMIN') or hasAuthority('ROLE_READ')")
public class AdminGrantController {

	private static final Set<String> PRIVILEGED_ROLES = Set.of(
			"SUPER_ADMIN", "AUTH_ADMIN", "AUDIT_ADMIN", "SECURITY", "COMPLIANCE");

	private final AdminRoleService adminRoleService;

	@GetMapping
	public GrantPageResponse getGrants(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return adminRoleService.getGrants(page, size);
	}

	@DeleteMapping("/{grantId}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN','AUTH_ADMIN') or hasAuthority('ROLE_WRITE')")
	public ResponseEntity<Void> revoke(@PathVariable UUID grantId, Authentication authentication) {
		var grant = adminRoleService.getGrant(grantId);
		boolean superAdmin = authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
		if (PRIVILEGED_ROLES.contains(grant.roleName()) && !superAdmin) {
			throw new AccessDeniedException("Only SUPER_ADMIN may revoke privileged platform roles");
		}
		adminRoleService.revokeRoleAssignment(
				grantId,
				(UUID) authentication.getPrincipal());
		return ResponseEntity.noContent().build();
	}
}
