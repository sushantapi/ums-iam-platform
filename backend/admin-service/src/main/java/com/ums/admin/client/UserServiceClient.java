package com.ums.admin.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.ums.admin.dto.response.UserSummaryPageResponse;
import com.ums.admin.dto.response.UserSummaryResponse;

@FeignClient(name = "user-service", contextId = "userClient")
public interface UserServiceClient {

	@GetMapping("/api/v1/internal/users")
	UserSummaryPageResponse getUsers(
			@RequestParam int page,
			@RequestParam int size,
			@RequestParam(required = false) String search);

	@GetMapping("/api/v1/internal/users/{userId}")
	UserSummaryResponse getUser(@PathVariable UUID userId);
}
