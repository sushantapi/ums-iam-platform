package com.ums.user.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ums.user.dto.UserResponse;

@FeignClient(name = "USER-SERVICE")
public interface UserClient {

	@GetMapping("/api/v1/internal/users/{userId}")
	UserResponse getUser(@PathVariable UUID userId);
}