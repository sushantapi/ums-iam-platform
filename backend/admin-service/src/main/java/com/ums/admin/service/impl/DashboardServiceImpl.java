package com.ums.admin.service.impl;

import org.springframework.stereotype.Service;

import com.ums.admin.dto.response.DashboardResponse;
import com.ums.admin.service.DashboardService;

@Service
public class DashboardServiceImpl implements DashboardService {

	@Override
	public DashboardResponse getDashboardSummary() {

		return DashboardResponse.builder().totalUsers(100).activeUsers(90).blockedUsers(10).activeSessions(40)
				.todayLogins(25).build();
	}
}