package com.ums.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ums.admin.dto.request.AssignRoleRequest;

@FeignClient(name = "role-service")
public interface RoleServiceClient {

	@PostMapping("/internal/roles/assign")
	void assignRole(@RequestBody AssignRoleRequest request);
}