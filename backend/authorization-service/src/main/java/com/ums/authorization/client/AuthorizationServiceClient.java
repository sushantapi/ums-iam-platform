package com.ums.authorization.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ums.authorization.dto.UserAuthorizationResponse;

@FeignClient(name = "authorization-service")
public interface AuthorizationServiceClient {

	@GetMapping("/internal/users/{userId}/authorization")
	UserAuthorizationResponse getUserAuthorization(@PathVariable UUID userId);
}