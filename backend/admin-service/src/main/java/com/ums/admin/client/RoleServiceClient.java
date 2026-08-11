package com.ums.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ums.admin.dto.request.AssignRoleRequest;

@FeignClient(name = "authorization-service", contextId = "roleClient")
public interface RoleServiceClient {

	@PostMapping("/api/v1/internal/roles/assign")
	void assignRole(@RequestBody AssignRoleRequest request);
}
