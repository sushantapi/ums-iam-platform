package com.ums.authorization.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ums.authorization.entity.UserRole;
import com.ums.authorization.repository.UserRoleRepository;
import com.ums.authorization.service.UserRoleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

	private final UserRoleRepository userRoleRepository;

	@Override
	public List<UserRole> getUserRoles(UUID userId) {

		return userRoleRepository.findByUserIdWithRole(userId);
	}

	@Override
	public List<UserRole> getActivePlatformUserRoles(UUID userId) {
		return userRoleRepository.findActivePlatformAssignments(userId);
	}

	@Override
	public List<UserRole> getActiveUserRoles(UUID userId, String scopeType, String scopeId) {
		if ("PLATFORM".equals(scopeType)) {
			return userRoleRepository.findActivePlatformAssignments(userId);
		}
		return userRoleRepository.findActiveAssignments(userId, scopeType, scopeId);
	}

	@Override
	public UserRole assignRole(UserRole userRole) {

		return userRoleRepository.save(userRole);
	}

	@Override
	public void revokeRoleAssignment(UUID assignmentId) {
		UserRole assignment = userRoleRepository.findById(assignmentId)
				.orElseThrow(() -> new com.ums.authorization.exception.RoleNotFoundException(
						"Role assignment not found"));
		assignment.setActive(false);
		userRoleRepository.save(assignment);
	}
}