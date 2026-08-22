package com.ums.org.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.ums.org.dto.UserResponse;
import com.ums.org.dto.UserSummaryPageResponse;

@FeignClient(name = "USER-SERVICE")
public interface UserClient {

	@GetMapping("/api/v1/internal/users/{userId}")
	UserResponse getUser(@PathVariable UUID userId);

	@GetMapping("/api/v1/internal/users")
	UserSummaryPageResponse getUsers(@RequestParam("page") int page,
			@RequestParam("size") int size,
			@RequestParam("search") String search);
}
