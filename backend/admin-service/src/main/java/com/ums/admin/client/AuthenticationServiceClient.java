package com.ums.admin.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import com.ums.admin.dto.response.UserAccountResponse;
import com.ums.admin.dto.response.UserMetricsResponse;

@FeignClient(name = "authentication-service", contextId = "authenticationAdminClient")
public interface AuthenticationServiceClient {

	@GetMapping("/api/v1/internal/auth/users/metrics")
	UserMetricsResponse getMetrics();

	@GetMapping("/api/v1/internal/auth/users/{userId}")
	UserAccountResponse getUser(@PathVariable UUID userId);

	@PostMapping("/api/v1/internal/auth/users/{userId}/activate")
	void activate(@PathVariable UUID userId, @RequestHeader("X-Actor-User-Id") UUID actorUserId);

	@PostMapping("/api/v1/internal/auth/users/{userId}/suspend")
	void suspend(@PathVariable UUID userId, @RequestHeader("X-Actor-User-Id") UUID actorUserId);

	@PostMapping("/api/v1/internal/auth/users/{userId}/unlock")
	void unlock(@PathVariable UUID userId, @RequestHeader("X-Actor-User-Id") UUID actorUserId);

	@PostMapping("/api/v1/internal/auth/users/{userId}/sessions/revoke-all")
	void revokeAllSessions(
			@PathVariable UUID userId,
			@RequestHeader("X-Actor-User-Id") UUID actorUserId);
}
