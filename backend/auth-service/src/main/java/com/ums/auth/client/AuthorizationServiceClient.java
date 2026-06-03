package com.ums.auth.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.ums.auth.dto.UserAuthorizationResponse;

@FeignClient(name = "AUTHORIZATION-SERVICE")
public interface AuthorizationServiceClient {

	@GetMapping("/internal/users/{userId}/authorization")
	UserAuthorizationResponse getUserAuthorization(@PathVariable UUID userId);

	@PostMapping("/internal/users/{userId}/roles/default")
	void assignDefaultRole(@PathVariable UUID userId);
}