package com.ums.admin.service.impl;

import org.springframework.stereotype.Service;

import com.ums.admin.client.RoleServiceClient;
import com.ums.admin.dto.request.AssignRoleRequest;
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
}
