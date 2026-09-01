package com.ums.authorization.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ums.authorization.config.InternalServiceFeignConfig;
import com.ums.authorization.dto.UserResponse;

@FeignClient(name = "user-service", contextId = "authorizationUserClient", configuration = InternalServiceFeignConfig.class)
public interface UserServiceClient {

	@GetMapping("/api/v1/internal/users/{userId}")
	UserResponse getUser(@PathVariable UUID userId);
}
