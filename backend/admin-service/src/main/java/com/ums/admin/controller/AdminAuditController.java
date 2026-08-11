package com.ums.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ums.admin.dto.response.AuditLogPageResponse;
import com.ums.admin.service.AdminAuditService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','AUDIT_ADMIN','SECURITY','COMPLIANCE') or hasAuthority('AUDIT_READ')")
public class AdminAuditController {

	private final AdminAuditService adminAuditService;

	@GetMapping("/logs")
	public AuditLogPageResponse getAuditLogs(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size,
			@RequestParam(required = false) String actor,
			@RequestParam(required = false) String target,
			@RequestParam(required = false) String eventType,
			@RequestParam(required = false) String serviceName) {
		validatePage(page, size);
		validateFilter(actor);
		validateFilter(target);
		validateFilter(eventType);
		validateFilter(serviceName);
		return adminAuditService.getAuditLogs(page, size, actor, target, eventType, serviceName);
	}

	private void validatePage(int page, int size) {
		if (page < 0 || page > 100_000 || size < 1 || size > 200) {
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid page or size");
		}
	}

	private void validateFilter(String value) {
		if (value != null && value.length() > 255) {
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.BAD_REQUEST, "Filter must not exceed 255 characters");
		}
	}
}
