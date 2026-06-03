package com.ums.authorization.service.impl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ums.authorization.dto.AssignRoleRequest;
import com.ums.authorization.dto.UserAuthorizationResponse;
import com.ums.authorization.entity.Role;
import com.ums.authorization.entity.RolePermission;
import com.ums.authorization.entity.UserRole;
import com.ums.authorization.exception.UserRoleAlreadyExistsException;
import com.ums.authorization.repository.RolePermissionRepository;
import com.ums.authorization.repository.RoleRepository;
import com.ums.authorization.repository.UserRoleRepository;
import com.ums.authorization.service.AuthorizationService;
import com.ums.authorization.service.RoleService;
import com.ums.authorization.service.UserRoleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthorizationServiceImpl implements AuthorizationService {

	private final RoleService roleService;

	private final UserRoleService userRoleService;

	private final RolePermissionRepository rolePermissionRepository;

	private final UserRoleRepository userRoleRepository;

	private final RoleRepository roleRepository;

	@Override
	public String assignRole(AssignRoleRequest request) {

		Role role = roleService.getRoleByName(request.getRoleName());

		boolean alreadyAssigned = userRoleRepository.existsByUserIdAndRole_Id(request.getUserId(), role.getId());

		if (alreadyAssigned) {

			throw new UserRoleAlreadyExistsException("Role already assigned to user");
		}

		UserRole userRole = UserRole.builder().userId(request.getUserId()).role(role).assignedAt(LocalDateTime.now())
				.build();

		userRoleService.assignRole(userRole);

		return "Role Assigned Successfully";
	}

	@Override
	public Set<String> getUserPermissions(UUID userId) {

		List<UserRole> userRoles = userRoleService.getUserRoles(userId);

		Set<String> permissions = new HashSet<>();

		for (UserRole userRole : userRoles) {

			List<RolePermission> rolePermissions = rolePermissionRepository.findByRole_Id(userRole.getRole().getId());

			for (RolePermission rolePermission : rolePermissions) {

				permissions.add(rolePermission.getPermission().getName());
			}
		}

		return permissions;
	}

	@Override
	public boolean hasPermission(UUID userId, String permission) {

		List<UserRole> userRoles = userRoleService.getUserRoles(userId);

		for (UserRole userRole : userRoles) {

			List<RolePermission> rolePermissions = rolePermissionRepository.findByRole_Id(userRole.getRole().getId());

			for (RolePermission rolePermission : rolePermissions) {

				if (

				rolePermission.getPermission().getName().equals(permission)

				) {

					return true;
				}
			}
		}

		return false;
	}

	@Override
	public UserAuthorizationResponse getUserAuthorization(UUID userId) {

		List<UserRole> userRoles = userRoleRepository.findByUserId(userId);

		List<String> roles = userRoles.stream()

				.map(userRole -> userRole.getRole().getName())

				.distinct()

				.toList();

		List<String> permissions =

				userRoles.stream()

						.flatMap(userRole ->

						rolePermissionRepository

								.findByRole(userRole.getRole())

								.stream())

						.map(rolePermission ->

						rolePermission

								.getPermission()

								.getName())

						.distinct()

						.toList();

		return UserAuthorizationResponse.builder().roles(roles).permissions(permissions).build();
	}

	@Override
	public void assignDefaultRole(UUID userId) {

		Role defaultRole = roleRepository

				.findByName("ROLE_USER")

				.orElseThrow(() ->

				new RuntimeException("Default role not found"));

		boolean alreadyAssigned =

				userRoleRepository.existsByUserIdAndRole_Id(userId, defaultRole.getId());

		if (alreadyAssigned) {
			return;
		}

		UserRole userRole = UserRole.builder()

				.userId(userId)

				.role(defaultRole)

				.build();

		userRoleRepository.save(userRole);
	}

}