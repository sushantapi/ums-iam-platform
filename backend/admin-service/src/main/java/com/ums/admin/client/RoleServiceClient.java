package com.ums.admin.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ums.admin.dto.request.AssignRoleRequest;
import com.ums.admin.dto.response.GrantPageResponse;
import com.ums.admin.dto.response.PermissionSummaryResponse;
import com.ums.admin.dto.response.RoleSummaryResponse;
import com.ums.admin.dto.response.UserRoleAssignmentResponse;

@FeignClient(name = "authorization-service", contextId = "roleClient")
public interface RoleServiceClient {

	@PostMapping("/api/v1/internal/roles/assign")
	void assignRole(@RequestBody AssignRoleRequest request);

	@GetMapping("/api/v1/internal/authorization/roles")
	List<RoleSummaryResponse> getRoles();

	@GetMapping("/api/v1/internal/authorization/roles/{roleId}")
	RoleSummaryResponse getRole(@PathVariable UUID roleId);

	@GetMapping("/api/v1/internal/authorization/roles/{roleId}/permissions")
	List<PermissionSummaryResponse> getRolePermissions(@PathVariable UUID roleId);

	@GetMapping("/api/v1/internal/authorization/permissions")
	List<PermissionSummaryResponse> getPermissions();

	@GetMapping("/api/v1/internal/authorization/users/{userId}/roles")
	List<UserRoleAssignmentResponse> getUserRoles(@PathVariable UUID userId);

	@GetMapping("/api/v1/internal/authorization/grants")
	GrantPageResponse getGrants(@org.springframework.web.bind.annotation.RequestParam int page,
			@org.springframework.web.bind.annotation.RequestParam int size);

	@GetMapping("/api/v1/internal/authorization/grants/{assignmentId}")
	UserRoleAssignmentResponse getGrant(@PathVariable UUID assignmentId);

	@DeleteMapping("/api/v1/internal/authorization/role-assignments/{assignmentId}")
	UUID revokeRoleAssignment(@PathVariable UUID assignmentId);
}
