package com.ums.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ums.auth.dto.CreateUserProfileRequest;

@FeignClient(name = "USER-SERVICE")
public interface UserServiceClient {

	@PostMapping("/api/v1/internal/users")
	void createUserProfile(@RequestBody CreateUserProfileRequest request);
}