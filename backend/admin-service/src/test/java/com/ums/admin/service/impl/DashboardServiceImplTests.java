package com.ums.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.ums.admin.dto.response.DashboardResponse;

class DashboardServiceImplTests {

	private final DashboardServiceImpl dashboardService = new DashboardServiceImpl();

	@Test
	void returnsTheNestedDashboardContract() {
		DashboardResponse response = dashboardService.getDashboardSummary();

		assertThat(response.users()).isEqualTo(new DashboardResponse.UserMetrics(0, 0, 0, 0));
		assertThat(response.organizations()).isEqualTo(new DashboardResponse.OrganizationMetrics(0, 0, 0));
		assertThat(response.roles()).isEqualTo(new DashboardResponse.RoleMetrics(0));
		assertThat(response.audit()).isEqualTo(new DashboardResponse.AuditMetrics(0, 0));
	}
}
