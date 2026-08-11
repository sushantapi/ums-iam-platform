package com.ums.admin.service.impl;

import org.springframework.stereotype.Service;

import com.ums.admin.client.UserServiceClient;
import com.ums.admin.dto.response.UserSummaryPageResponse;
import com.ums.admin.service.AdminUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

	private final UserServiceClient userServiceClient;

	@Override
	public UserSummaryPageResponse getUsers(int page, int size, String search) {
		return userServiceClient.getUsers(page, size, search);
	}

	/*
	 * @Override public UserDetailResponse getUserById(Long id) { return
	 * userServiceClient.getUserById(id); }
	 * 
	 * @Override public String blockUser(Long id) {
	 * 
	 * userServiceClient.blockUser(id);
	 * 
	 * return "User blocked successfully"; }
	 * 
	 * @Override public String activateUser(Long id) {
	 * 
	 * userServiceClient.activateUser(id);
	 * 
	 * return "User activated successfully"; }
	 */
}
