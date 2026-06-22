package com.ums.auth.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ums.auth.dto.admin.AdminSessionFilter;
import com.ums.auth.dto.admin.AdminSessionPageResponse;
import com.ums.auth.dto.admin.AdminSessionResponse;
import com.ums.auth.service.AdminSessionService;
import com.ums.security.annotation.CurrentUser;
import com.ums.security.dto.JwtUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ORG_ADMIN')")
@RequiredArgsConstructor
public class AdminSessionController {

	private final AdminSessionService adminSessionService;

	@GetMapping("/sessions")
	public AdminSessionPageResponse getSessions(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) UUID userId,
			@RequestParam(required = false) UUID organizationId,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		AdminSessionFilter filter = new AdminSessionFilter(userId, organizationId, status, from, to);
		return AdminSessionPageResponse.from(adminSessionService.listSessions(filter, PageRequest.of(page, size)));
	}

	@GetMapping("/users/{userId}/sessions")
	public List<AdminSessionResponse> getUserSessions(@PathVariable UUID userId) {
		return adminSessionService.listUserSessions(userId);
	}

	@PostMapping("/sessions/{sessionId}/revoke")
	public ResponseEntity<Void> revokeSession(
			@PathVariable UUID sessionId,
			@CurrentUser JwtUser adminUser) {
		adminSessionService.revokeSession(sessionId, adminUser.getUserId());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/users/{userId}/sessions/revoke-all")
	public ResponseEntity<Void> revokeAllUserSessions(
			@PathVariable UUID userId,
			@CurrentUser JwtUser adminUser) {
		adminSessionService.revokeAllUserSessions(userId, adminUser.getUserId());
		return ResponseEntity.noContent().build();
	}
}
