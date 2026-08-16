package com.ums.auth.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.auth.dto.admin.AdminUserAccountResponse;
import com.ums.auth.dto.admin.AdminUserMetricsResponse;
import com.ums.auth.service.AdminSessionService;
import com.ums.auth.service.AdminUserAccountService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/auth/users")
@RequiredArgsConstructor
public class InternalUserAdminController {

	private static final String ACTOR_USER_HEADER = "X-Actor-User-Id";

	private final AdminSessionService adminSessionService;
	private final AdminUserAccountService adminUserAccountService;

	@GetMapping("/metrics")
	public AdminUserMetricsResponse getMetrics() {
		return adminUserAccountService.getMetrics();
	}

	@GetMapping("/{userId}")
	public AdminUserAccountResponse getUser(@PathVariable UUID userId) {
		return adminUserAccountService.getUser(userId);
	}

	@PostMapping("/{userId}/activate")
	public ResponseEntity<Void> activate(@PathVariable UUID userId,
			@RequestHeader(ACTOR_USER_HEADER) UUID actorUserId) {
		adminUserAccountService.activateUser(userId, actorUserId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{userId}/suspend")
	public ResponseEntity<Void> suspend(@PathVariable UUID userId,
			@RequestHeader(ACTOR_USER_HEADER) UUID actorUserId) {
		adminUserAccountService.suspendUser(userId, actorUserId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{userId}/unlock")
	public ResponseEntity<Void> unlock(@PathVariable UUID userId,
			@RequestHeader(ACTOR_USER_HEADER) UUID actorUserId) {
		adminUserAccountService.unlockUser(userId, actorUserId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{userId}/sessions/revoke-all")
	public ResponseEntity<Void> revokeAllSessions(
			@PathVariable UUID userId,
			@RequestHeader(ACTOR_USER_HEADER) UUID actorUserId) {
		adminSessionService.revokeAllUserSessions(userId, actorUserId);
		return ResponseEntity.noContent().build();
	}
}
