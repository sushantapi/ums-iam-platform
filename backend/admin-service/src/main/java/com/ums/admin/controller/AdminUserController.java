package com.ums.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ums.admin.dto.response.UserSummaryPageResponse;
import com.ums.admin.service.AdminUserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','USER_ADMIN','SUPPORT') or hasAuthority('USER_READ')")
public class AdminUserController {

	private final AdminUserService adminUserService;

	@GetMapping
	public UserSummaryPageResponse getUsers(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String search,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String organizationId,
			@RequestParam(required = false) String role) {
		validatePage(page, size);
		validateFilter("search", search);
		validateFilter("status", status);
		validateFilter("organizationId", organizationId);
		validateFilter("role", role);
		if (hasText(status) || hasText(organizationId) || hasText(role)) {
			throw badRequest("status, organizationId, and role filters are not supported by the current user profile schema");
		}
		return adminUserService.getUsers(page, size, search);
	}

	private void validatePage(int page, int size) {
		if (page < 0 || page > 100_000) {
			throw badRequest("page must be between 0 and 100000");
		}
		if (size < 1 || size > 200) {
			throw badRequest("size must be between 1 and 200");
		}
	}

	private void validateFilter(String name, String value) {
		if (value != null && value.length() > 255) {
			throw badRequest(name + " must not exceed 255 characters");
		}
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private org.springframework.web.server.ResponseStatusException badRequest(String reason) {
		return new org.springframework.web.server.ResponseStatusException(
				org.springframework.http.HttpStatus.BAD_REQUEST, reason);
	}

	/*
	 * @GetMapping("/{id}") public UserDetailResponse getUserById(@PathVariable Long
	 * id) {
	 * 
	 * return adminUserService.getUserById(id); }
	 * 
	 * @PatchMapping("/{id}/block") public String blockUser(@PathVariable Long id) {
	 * 
	 * return adminUserService.blockUser(id); }
	 * 
	 * @PatchMapping("/{id}/activate") public String activateUser(@PathVariable Long
	 * id) {
	 * 
	 * return adminUserService.activateUser(id); }
	 */
}
