package com.ums.admin.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.ums.admin.dto.response.UserSummaryResponse;

@FeignClient(name = "user-service", contextId = "userClient")
public interface UserServiceClient {

	@GetMapping("/api/v1/internal/users")
	List<UserSummaryResponse> getAllUsers();
}