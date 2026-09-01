package com.ums.authorization.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ums.authorization.client.UserServiceClient;
import com.ums.authorization.dto.AssignPermissionRequest;
import com.ums.authorization.dto.AssignRoleRequest;
import com.ums.authorization.dto.UserResponse;
import com.ums.authorization.dto.UserAuthorizationResponse;
import com.ums.authorization.dto.UserPermissionsResponse;
import com.ums.authorization.entity.Permission;
import com.ums.authorization.entity.Role;
import com.ums.authorization.entity.RolePermission;
import com.ums.authorization.entity.UserRole;
import com.ums.authorization.exception.UserRoleAlreadyExistsException;
import com.ums.authorization.publisher.RoleEventPublisher;
import com.ums.authorization.repository.PermissionRepository;
import com.ums.authorization.repository.RolePermissionRepository;
import com.ums.authorization.repository.RoleRepository;
import com.ums.authorization.repository.UserRoleRepository;
import com.ums.authorization.service.AuthorizationService;
import com.ums.authorization.service.RoleService;
import com.ums.authorization.service.UserRoleService;
import com.ums.events.event.role.RoleAssignedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationServiceImpl implements AuthorizationService {

	private final RoleService roleService;

	private final UserRoleService userRoleService;

	private final RolePermissionRepository rolePermissionRepository;

	private final PermissionRepository permissionRepository;

	private final UserRoleRepository userRoleRepository;

	private final RoleRepository roleRepository;

	private final RoleEventPublisher roleEventPublisher;

	private final UserServiceClient userServiceClient;

	@Override
	public String assignRole(AssignRoleRequest request) {

		Role role = roleService.getRoleByName(request.getRoleName());
		String scopeType = normalizeScopeType(request.getScopeType());
		String scopeId = normalizeScopeId(request.getScopeId());

		boolean alreadyAssigned = userRoleRepository.existsByUserIdAndRole_IdAndScopeTypeAndScopeIdAndActiveTrue(
				request.getUserId(), role.getId(), scopeType, scopeId);

		if (alreadyAssigned) {

			throw new UserRoleAlreadyExistsException("Role already assigned to user");
		}

		UserRole userRole = UserRole.builder()
				.userId(request.getUserId())
				.role(role)
				.assignedBy(request.getAssignedBy())
				.scopeType(scopeType)
				.scopeId(scopeId)
				.active(true)
				.assignedAt(LocalDateTime.now())
				.build();

		/*
		 * userRoleService.assignRole(userRole);
		 * 
		 * return "Role Assigned Successfully";
		 */

		userRoleService.assignRole(userRole);

		RoleAssignedEvent event = buildRoleAssignedEvent(
				request.getUserId(),
				role,
				request.getAssignedBy());

		roleEventPublisher.publishRoleAssigned(event);

		return "Role Assigned Successfully";
	}

	private String normalizeScopeType(String scopeType) {
		if (scopeType == null || scopeType.isBlank()) {
			return "PLATFORM";
		}
		return scopeType.trim().toUpperCase();
	}

	private String normalizeScopeId(String scopeId) {
		if (scopeId == null || scopeId.isBlank()) {
			return "*";
		}
		return scopeId.trim();
	}

	@Override
	public UserPermissionsResponse getUserPermissions(UUID userId) {

		Set<String> permissions = userRoleService.getUserRoles(userId).stream()
				.flatMap(userRole -> rolePermissionRepository.findByRole(userRole.getRole()).stream())
				.map(rolePermission -> rolePermission.getPermission().getCode()).collect(Collectors.toSet());

		return UserPermissionsResponse.builder().userId(userId).permissions(permissions).build();
	}

	/*
	 * @Override public boolean hasPermission(UUID userId, String permissionCode) {
	 * 
	 * List<UserRole> userRoles = userRoleService.getUserRoles(userId);
	 * 
	 * for (UserRole userRole : userRoles) {
	 * 
	 * List<RolePermission> rolePermissions =
	 * rolePermissionRepository.findByRole_Id(userRole.getRole().getId());
	 * 
	 * for (RolePermission rolePermission : rolePermissions) {
	 * 
	 * if
	 * (rolePermission.getPermission().getCode().equalsIgnoreCase(permissionCode)) {
	 * 
	 * return true; } } }
	 * 
	 * return false; }
	 */

	@Override
	public boolean hasPermission(UUID userId, String permissionCode) {

		return getUserPermissions(userId).getPermissions().contains(permissionCode);
	}

	@Override
	public UserAuthorizationResponse getUserAuthorization(UUID userId) {

		List<UserRole> userRoles = userRoleRepository.findByUserId(userId);

		List<String> roles = userRoles.stream().map(userRole -> userRole.getRole().getName()).distinct().toList();

		List<String> permissions = userRoles.stream()
				.flatMap(userRole -> rolePermissionRepository.findByRole(userRole.getRole()).stream())
				.map(rolePermission -> rolePermission.getPermission().getCode()).distinct().toList();

		// return
		// UserAuthorizationResponse.builder().roles(roles).permissions(permissions).build();
		return UserAuthorizationResponse.builder().userId(userId).roles(roles).permissions(permissions).build();
	}

	@Override
	public void assignDefaultRole(UUID userId) {

		log.info("Assigning default role to user {}", userId);

		Role defaultRole = roleRepository.findByNameIgnoreCase("EMPLOYEE")
				.orElseThrow(() -> new RuntimeException("Default role not found"));

		boolean alreadyAssigned = userRoleRepository.existsByUserIdAndRole_Id(userId, defaultRole.getId());

		if (alreadyAssigned) {

			log.info("Default role already assigned for user {}", userId);
			return;
		}

		UserRole userRole = UserRole.builder().userId(userId).role(defaultRole).scopeType("PLATFORM").scopeId("*")
				.active(true).assignedAt(LocalDateTime.now()).build();

		userRoleRepository.save(userRole);

		RoleAssignedEvent event = buildRoleAssignedEvent(userId, defaultRole, null);

		roleEventPublisher.publishRoleAssigned(event);

		log.info("Default role assigned successfully for user {}", userId);
	}

	@Override
	public String assignPermission(AssignPermissionRequest request) {

		Role role = roleRepository.findById(request.getRoleId())
				.orElseThrow(() -> new RuntimeException("Role not found"));

		Permission permission = permissionRepository.findById(request.getPermissionId())
				.orElseThrow(() -> new RuntimeException("Permission not found"));

		boolean exists = rolePermissionRepository.existsByRole_IdAndPermission_Id(role.getId(), permission.getId());

		if (exists) {
			return "Permission already assigned";
		}

		RolePermission rolePermission = RolePermission.builder().role(role).permission(permission)
				.assignedAt(LocalDateTime.now()).build();

		rolePermissionRepository.save(rolePermission);

		return "Permission assigned successfully";
	}

	private RoleAssignedEvent buildRoleAssignedEvent(UUID userId, Role role, UUID assignedBy) {
		UserResponse recipient = resolveRecipient(userId);

		return RoleAssignedEvent.builder()
				.userId(userId)
				.roleId(role.getId())
				.roleName(role.getName())
				.email(recipient == null ? null : recipient.email())
				.firstName(recipient == null ? null : recipient.firstName())
				.assignedBy(assignedBy)
				.assignedAt(LocalDateTime.now())
				.build();
	}

	private UserResponse resolveRecipient(UUID userId) {
		try {
			return userServiceClient.getUser(userId);
		} catch (RuntimeException ex) {
			log.warn("Unable to enrich RoleAssignedEvent with recipient details for userId={}", userId, ex);
			return null;
		}
	}

}
