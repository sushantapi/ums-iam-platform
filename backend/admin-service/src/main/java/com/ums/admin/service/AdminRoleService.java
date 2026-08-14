package com.ums.admin.service;

import java.util.List;
import java.util.UUID;

import com.ums.admin.dto.request.AssignRoleRequest;
import com.ums.admin.dto.response.GrantPageResponse;
import com.ums.admin.dto.response.PermissionSummaryResponse;
import com.ums.admin.dto.response.RoleSummaryResponse;
import com.ums.admin.dto.response.UserRoleAssignmentResponse;

public interface AdminRoleService {

	String assignRole(AssignRoleRequest request);

	List<RoleSummaryResponse> getRoles();

	RoleSummaryResponse getRole(UUID roleId);

	List<PermissionSummaryResponse> getRolePermissions(UUID roleId);

	List<PermissionSummaryResponse> getPermissions();

	List<UserRoleAssignmentResponse> getUserRoles(UUID userId);

	GrantPageResponse getGrants(int page, int size);

	UserRoleAssignmentResponse getGrant(UUID assignmentId);

	void revokeRoleAssignment(UUID assignmentId);

}
