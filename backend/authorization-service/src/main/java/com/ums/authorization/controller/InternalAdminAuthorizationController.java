package com.ums.authorization.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ums.authorization.dto.admin.GrantPageResponse;
import com.ums.authorization.dto.admin.PermissionSummaryResponse;
import com.ums.authorization.dto.admin.RoleSummaryResponse;
import com.ums.authorization.dto.admin.UserRoleAssignmentResponse;
import com.ums.authorization.entity.UserRole;
import com.ums.authorization.repository.PermissionRepository;
import com.ums.authorization.repository.RolePermissionRepository;
import com.ums.authorization.repository.RoleRepository;
import com.ums.authorization.repository.UserRoleRepository;
import com.ums.authorization.service.UserRoleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/authorization")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
public class InternalAdminAuthorizationController {

	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;
	private final RolePermissionRepository rolePermissionRepository;
	private final UserRoleRepository userRoleRepository;
	private final UserRoleService userRoleService;

	@GetMapping("/roles")
	public List<RoleSummaryResponse> getRoles() {
		return roleRepository.findAll().stream().map(this::toRole).toList();
	}

	@GetMapping("/roles/{roleId}")
	public RoleSummaryResponse getRole(@PathVariable UUID roleId) {
		return roleRepository.findById(roleId).map(this::toRole)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
	}

	@GetMapping("/roles/{roleId}/permissions")
	public List<PermissionSummaryResponse> getRolePermissions(@PathVariable UUID roleId) {
		if (!roleRepository.existsById(roleId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found");
		}
		return rolePermissionRepository.findByRoleIdWithPermission(roleId).stream()
				.map(rolePermission -> toPermission(rolePermission.getPermission())).toList();
	}

	@GetMapping("/permissions")
	public List<PermissionSummaryResponse> getPermissions() {
		return permissionRepository.findAll().stream().map(this::toPermission).toList();
	}

	@GetMapping("/users/{userId}/roles")
	public List<UserRoleAssignmentResponse> getUserRoles(@PathVariable UUID userId) {
		return userRoleService.getUserRoles(userId).stream().map(this::toAssignment).toList();
	}

	@GetMapping("/grants")
	public GrantPageResponse getGrants(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		validatePage(page, size);
		var assignments = userRoleRepository.findAllWithRole(
				PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assignedAt")));
		return new GrantPageResponse(assignments.getContent().stream().map(this::toAssignment).toList(),
				assignments.getNumber(), assignments.getSize(), assignments.getTotalElements(), assignments.getTotalPages());
	}

	@GetMapping("/grants/{assignmentId}")
	public UserRoleAssignmentResponse getGrant(@PathVariable UUID assignmentId) {
		UserRole assignment = userRoleRepository.findByIdWithRole(assignmentId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role assignment not found"));
		return toAssignment(assignment);
	}

	@DeleteMapping("/role-assignments/{assignmentId}")
	public UUID revokeRoleAssignment(
			@PathVariable UUID assignmentId,
			@RequestHeader("X-Actor-User-Id") UUID actorUserId) {
		return userRoleService.revokeRoleAssignment(assignmentId, actorUserId);
	}

	private RoleSummaryResponse toRole(com.ums.authorization.entity.Role role) {
		return new RoleSummaryResponse(role.getId(), role.getName(), role.getDescription(),
				Boolean.TRUE.equals(role.getIsSystem()), Boolean.TRUE.equals(role.getActive()));
	}

	private PermissionSummaryResponse toPermission(com.ums.authorization.entity.Permission permission) {
		return new PermissionSummaryResponse(permission.getId(), permission.getCode(), permission.getAction(),
				permission.getDescription(), Boolean.TRUE.equals(permission.getActive()));
	}

	private UserRoleAssignmentResponse toAssignment(UserRole assignment) {
		return new UserRoleAssignmentResponse(assignment.getId(), assignment.getRole().getId(),
				assignment.getRole().getName(), assignment.getScopeType(), assignment.getScopeId(),
				Boolean.TRUE.equals(assignment.getActive()), assignment.getAssignedAt(), assignment.getExpiresAt());
	}

	private void validatePage(int page, int size) {
		if (page < 0 || page > 100_000 || size < 1 || size > 200) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page or size");
		}
	}
}
