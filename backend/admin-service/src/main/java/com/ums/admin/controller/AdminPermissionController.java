package com.ums.admin.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.admin.dto.response.PermissionSummaryResponse;
import com.ums.admin.service.AdminRoleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','AUTH_ADMIN','SUPPORT') or hasAuthority('ROLE_READ')")
public class AdminPermissionController {

	private final AdminRoleService adminRoleService;

	@GetMapping
	public List<PermissionSummaryResponse> getPermissions() {
		return adminRoleService.getPermissions();
	}
}
