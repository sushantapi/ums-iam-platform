package com.ums.notification.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.ums.notification.dto.UserDirectoryResponse;

@FeignClient(name = "USER-SERVICE")
public interface UserDirectoryClient {

	@GetMapping("/api/v1/internal/users/{userId}")
	UserDirectoryResponse getUser(
			@PathVariable UUID userId,
			@RequestHeader("X-Internal-Service-Secret") String internalServiceSecret);
}
