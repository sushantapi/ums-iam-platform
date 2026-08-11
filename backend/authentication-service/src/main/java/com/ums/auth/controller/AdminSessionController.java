package com.ums.auth.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminSessionController {

	private static final int MAX_PAGE = 100_000;
	private static final int MAX_PAGE_SIZE = 200;

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
		if (page < 0 || page > MAX_PAGE) {
			throw badRequest("page must be between 0 and " + MAX_PAGE);
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw badRequest("size must be between 1 and " + MAX_PAGE_SIZE);
		}
		if (status != null && !status.isBlank()
				&& !"ACTIVE".equalsIgnoreCase(status)
				&& !"REVOKED".equalsIgnoreCase(status)
				&& !"EXPIRED".equalsIgnoreCase(status)) {
			throw badRequest("status must be ACTIVE, REVOKED, or EXPIRED");
		}
		if (from != null && to != null && from.isAfter(to)) {
			throw badRequest("'from' must be on or before 'to'");
		}
		if (LocalDate.MAX.equals(to)) {
			throw badRequest("'to' is outside the supported date range");
		}
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
			Authentication authentication) {
		adminSessionService.revokeSession(sessionId, currentUserId(authentication));
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/users/{userId}/sessions/revoke-all")
	public ResponseEntity<Void> revokeAllUserSessions(
			@PathVariable UUID userId,
			Authentication authentication) {
		adminSessionService.revokeAllUserSessions(userId, currentUserId(authentication));
		return ResponseEntity.noContent().build();
	}

	private UUID currentUserId(Authentication authentication) {
		return (UUID) authentication.getPrincipal();
	}

	private org.springframework.web.server.ResponseStatusException badRequest(String reason) {
		return new org.springframework.web.server.ResponseStatusException(
				org.springframework.http.HttpStatus.BAD_REQUEST, reason);
	}
}
