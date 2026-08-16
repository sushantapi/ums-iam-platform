package com.ums.authorization.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ums.authorization.dto.AssignPermissionRequest;
import com.ums.authorization.dto.AssignRoleRequest;
import com.ums.authorization.dto.UserAuthorizationResponse;
import com.ums.authorization.dto.UserPermissionsResponse;
import com.ums.authorization.entity.Permission;
import com.ums.authorization.entity.Role;
import com.ums.authorization.entity.RolePermission;
import com.ums.authorization.entity.UserRole;
import com.ums.authorization.exception.PermissionNotFoundException;
import com.ums.authorization.exception.RoleNotFoundException;
import com.ums.authorization.exception.UserRoleAlreadyExistsException;
import com.ums.authorization.publisher.RoleEventPublisher;
import com.ums.authorization.repository.PermissionRepository;
import com.ums.authorization.repository.RolePermissionRepository;
import com.ums.authorization.repository.RoleRepository;
import com.ums.authorization.repository.UserRoleRepository;
import com.ums.authorization.service.AuthorizationService;
import com.ums.authorization.service.RoleService;
import com.ums.authorization.service.UserRoleService;
import com.ums.events.event.AuditEvent;
import com.ums.events.event.role.RoleAssignedEvent;
import com.ums.events.publisher.AuditPublisher;

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

        private final AuditPublisher auditPublisher;

	@Override
	public String assignRole(AssignRoleRequest request) {

		Role role = roleService.getRoleByName(request.getRoleName());

		String scopeType = normalizeScopeType(request.getScopeType());
		String scopeId = normalizeScopeId(scopeType, request.getScopeId());

		boolean alreadyAssigned = userRoleRepository
				.existsByUserIdAndRole_IdAndScopeTypeAndScopeIdAndActiveTrue(
						request.getUserId(), role.getId(), scopeType, scopeId);

		if (alreadyAssigned) {
			throw new UserRoleAlreadyExistsException("Role already assigned to user in this scope");
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

                UserRole savedAssignment = userRoleService.assignRole(userRole);

                publishAuditEvent(AuditEvent.builder()
                                .eventType("role.assigned")
                                .serviceName("authorization-service")
                                .userId(request.getAssignedBy() == null ? null : request.getAssignedBy().toString())
                                .action("ROLE_ASSIGN")
                                .entityType("ROLE_ASSIGNMENT")
                                .entityId(savedAssignment.getId().toString())
                                .details("Assigned role " + role.getName() + " to user " + request.getUserId()
                                                + " in scope " + scopeType + ":" + scopeId)
                                .timestamp(LocalDateTime.now())
                                .build());

                RoleAssignedEvent event = RoleAssignedEvent.builder()
                                .userId(request.getUserId())
                                .roleId(role.getId())
                                .roleName(role.getName())
                                .assignedBy(request.getAssignedBy())
                                .assignedAt(LocalDateTime.now())
                                .build();

                roleEventPublisher.publishRoleAssigned(event);

		return "Role Assigned Successfully";
	}

	@Override
	public UserPermissionsResponse getUserPermissions(UUID userId) {
		return getUserPermissions(userId, "PLATFORM", "*");
	}

	@Override
	public UserPermissionsResponse getUserPermissions(UUID userId, String scopeType, String scopeId) {
		String normalizedScopeType = normalizeScopeType(scopeType);
		String normalizedScopeId = normalizeScopeId(normalizedScopeType, scopeId);

		Set<String> permissions = userRoleService
				.getActiveUserRoles(userId, normalizedScopeType, normalizedScopeId).stream()
				.flatMap(userRole -> rolePermissionRepository.findByRoleIdWithPermission(userRole.getRole().getId()).stream())
				.filter(rolePermission -> Boolean.TRUE.equals(rolePermission.getPermission().getActive()))
				.map(rolePermission -> rolePermission.getPermission().getCode())
				.collect(Collectors.toSet());

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
		return hasPermission(userId, permissionCode, "PLATFORM", "*");
	}

	@Override
	public boolean hasPermission(UUID userId, String permissionCode, String scopeType, String scopeId) {
		return getUserPermissions(userId, scopeType, scopeId).getPermissions().contains(permissionCode);
	}

	@Override
	public UserAuthorizationResponse getUserAuthorization(UUID userId) {
		return getUserAuthorization(userId, "PLATFORM", "*");
	}

	@Override
	public UserAuthorizationResponse getUserAuthorization(UUID userId, String scopeType, String scopeId) {
		String normalizedScopeType = normalizeScopeType(scopeType);
		String normalizedScopeId = normalizeScopeId(normalizedScopeType, scopeId);

		List<UserRole> userRoles = userRoleService.getActiveUserRoles(userId, normalizedScopeType, normalizedScopeId);

		List<String> roles = userRoles.stream().map(userRole -> userRole.getRole().getName()).distinct().toList();

		List<String> permissions = userRoles.stream()
				.flatMap(userRole -> rolePermissionRepository.findByRoleIdWithPermission(userRole.getRole().getId()).stream())
				.filter(rolePermission -> Boolean.TRUE.equals(rolePermission.getPermission().getActive()))
				.map(rolePermission -> rolePermission.getPermission().getCode()).distinct().toList();

		return UserAuthorizationResponse.builder().userId(userId).roles(roles).permissions(permissions).build();
	}

	@Override
	public void assignDefaultRole(UUID userId) {

		log.info("Assigning default role to user {}", userId);

		Role defaultRole = roleRepository.findByNameIgnoreCase("EMPLOYEE")
				.orElseThrow(() -> new RoleNotFoundException("Default role not found"));

		boolean alreadyAssigned = userRoleRepository
				.existsByUserIdAndRole_IdAndScopeTypeAndScopeIdAndActiveTrue(
						userId, defaultRole.getId(), "PLATFORM", "*");

		if (alreadyAssigned) {

			log.info("Default role already assigned for user {}", userId);
			return;
		}

		UserRole userRole = UserRole.builder().userId(userId).role(defaultRole).scopeType("PLATFORM").scopeId("*")
				.active(true).assignedAt(LocalDateTime.now()).build();

		userRoleRepository.save(userRole);

		RoleAssignedEvent event = RoleAssignedEvent.builder().userId(userId).roleId(defaultRole.getId())
				.roleName(defaultRole.getName()).assignedAt(LocalDateTime.now()).build();

		roleEventPublisher.publishRoleAssigned(event);

		log.info("Default role assigned successfully for user {}", userId);
	}

	@Override
	public String assignPermission(AssignPermissionRequest request) {

		Role role = roleRepository.findById(request.getRoleId())
				.orElseThrow(() -> new RoleNotFoundException("Role not found"));

		Permission permission = permissionRepository.findById(request.getPermissionId())
				.orElseThrow(() -> new PermissionNotFoundException("Permission not found"));

		boolean exists = rolePermissionRepository.existsByRole_IdAndPermission_Id(role.getId(), permission.getId());

		if (exists) {
			return "Permission already assigned";
		}

		RolePermission rolePermission = RolePermission.builder().role(role).permission(permission)
				.assignedAt(LocalDateTime.now()).build();

		rolePermissionRepository.save(rolePermission);

		return "Permission assigned successfully";
	}

        private void publishAuditEvent(AuditEvent event) {
                try {
                        auditPublisher.publish(event);
                } catch (Exception ex) {
                        log.error(
                                        "Failed to publish RBAC audit event eventType={} entityId={}",
                                        event.getEventType(),
                                        event.getEntityId(),
                                        ex);
                }
        }

        private String normalizeScopeType(String scopeType) {
		if (scopeType == null || scopeType.isBlank()) {
			return "PLATFORM";
		}

		String normalized = scopeType.trim().toUpperCase();
		if (!Set.of("PLATFORM", "ORG", "DEPARTMENT").contains(normalized)) {
			throw new IllegalArgumentException("Unsupported role scope: " + scopeType);
		}
		return normalized;
	}

	private String normalizeScopeId(String scopeType, String scopeId) {
		if ("PLATFORM".equals(scopeType)) {
			return "*";
		}

		if (scopeId == null || scopeId.isBlank() || "*".equals(scopeId.trim())) {
			throw new IllegalArgumentException(scopeType + " role assignments require a concrete scopeId");
		}

		return scopeId.trim();
	}

}