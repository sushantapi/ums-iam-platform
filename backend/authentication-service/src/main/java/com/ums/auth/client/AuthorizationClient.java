package com.ums.auth.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.ums.auth.dto.UserAuthorizationResponse;

@FeignClient(name = "authorization-service")
public interface AuthorizationClient {

	@GetMapping("/api/v1/internal/users/{userId}/authorization")
	UserAuthorizationResponse getAuthorization(@PathVariable UUID userId);

	@PostMapping("/api/v1/internal/users/{userId}/roles/default")
	void assignDefaultRole(@PathVariable UUID userId);
}
