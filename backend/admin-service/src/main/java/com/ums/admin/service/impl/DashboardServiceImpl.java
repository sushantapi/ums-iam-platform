package com.ums.admin.service.impl;

import org.springframework.stereotype.Service;

import com.ums.admin.dto.response.DashboardResponse;
import com.ums.admin.service.DashboardService;

@Service
public class DashboardServiceImpl implements DashboardService {

	@Override
	public DashboardResponse getDashboardSummary() {
		return new DashboardResponse(
				new DashboardResponse.UserMetrics(0, 0, 0, 0),
				new DashboardResponse.OrganizationMetrics(0, 0, 0),
				new DashboardResponse.RoleMetrics(0),
				new DashboardResponse.AuditMetrics(0, 0));
	}
}
