package com.ums.admin.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ums.admin.client.RoleServiceClient;
import com.ums.admin.dto.request.AssignRoleRequest;
import com.ums.admin.dto.response.GrantPageResponse;
import com.ums.admin.dto.response.PermissionSummaryResponse;
import com.ums.admin.dto.response.RoleSummaryResponse;
import com.ums.admin.dto.response.UserRoleAssignmentResponse;
import com.ums.admin.service.AdminRoleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminRoleServiceImpl implements AdminRoleService {

	private final RoleServiceClient roleServiceClient;

	@Override
	public String assignRole(AssignRoleRequest request) {

		roleServiceClient.assignRole(request);

		return "Role assigned successfully";
	}

	@Override
	public List<RoleSummaryResponse> getRoles() {
		return roleServiceClient.getRoles();
	}

	@Override
	public RoleSummaryResponse getRole(UUID roleId) {
		return roleServiceClient.getRole(roleId);
	}

	@Override
	public List<PermissionSummaryResponse> getRolePermissions(UUID roleId) {
		return roleServiceClient.getRolePermissions(roleId);
	}

	@Override
	public List<PermissionSummaryResponse> getPermissions() {
		return roleServiceClient.getPermissions();
	}

	@Override
	public List<UserRoleAssignmentResponse> getUserRoles(UUID userId) {
		return roleServiceClient.getUserRoles(userId);
	}

	@Override
	public GrantPageResponse getGrants(int page, int size) {
		return roleServiceClient.getGrants(page, size);
	}

	@Override
	public UserRoleAssignmentResponse getGrant(UUID assignmentId) {
		return roleServiceClient.getGrant(assignmentId);
	}

	@Override
	public void revokeRoleAssignment(UUID assignmentId) {
		roleServiceClient.revokeRoleAssignment(assignmentId);
	}
}
