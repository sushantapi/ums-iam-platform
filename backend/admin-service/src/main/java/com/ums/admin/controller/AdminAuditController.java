package com.ums.admin.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.admin.dto.response.AuditLogResponse;
import com.ums.admin.service.AdminAuditService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminAuditController {

	private final AdminAuditService adminAuditService;

	@GetMapping("/logs")
	public List<AuditLogResponse> getAuditLogs() {

		return adminAuditService.getAuditLogs();
	}
}