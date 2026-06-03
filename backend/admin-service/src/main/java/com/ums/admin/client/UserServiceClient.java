package com.ums.admin.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.ums.admin.dto.response.UserSummaryResponse;

@FeignClient(name = "user-service")
public interface UserServiceClient {

	@GetMapping("/api/v1/internal/users")
	List<UserSummaryResponse> getAllUsers();

	/*
	 * @GetMapping("/api/v1/internal/users/{id}") UserDetailResponse
	 * getUserById(@PathVariable Long id);
	 * 
	 * @PatchMapping("/api/v1/internal/users/{id}/block") void
	 * blockUser(@PathVariable Long id);
	 * 
	 * @PatchMapping("/api/v1/internal/users/{id}/activate") void
	 * activateUser(@PathVariable Long id);
	 */
}