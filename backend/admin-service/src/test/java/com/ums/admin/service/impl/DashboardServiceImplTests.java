package com.ums.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ums.admin.client.AuditServiceClient;
import com.ums.admin.client.AuthenticationServiceClient;
import com.ums.admin.client.OrganizationServiceClient;
import com.ums.admin.client.RoleServiceClient;
import com.ums.admin.dto.response.AuditMetricsResponse;
import com.ums.admin.dto.response.DashboardResponse;
import com.ums.admin.dto.response.OrganizationMetricsResponse;
import com.ums.admin.dto.response.RoleSummaryResponse;
import com.ums.admin.dto.response.UserMetricsResponse;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTests {

    @Mock private AuthenticationServiceClient authenticationServiceClient;
    @Mock private OrganizationServiceClient organizationServiceClient;
    @Mock private RoleServiceClient roleServiceClient;
    @Mock private AuditServiceClient auditServiceClient;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    void returnsAggregatedBackendMetrics() {
        when(authenticationServiceClient.getMetrics()).thenReturn(new UserMetricsResponse(10, 8, 1, 1));
        when(organizationServiceClient.getMetrics()).thenReturn(new OrganizationMetricsResponse(4, 3, 0));
        when(roleServiceClient.getRoles()).thenReturn(List.of(
                new RoleSummaryResponse(null, "EMPLOYEE", "Employee", true, true),
                new RoleSummaryResponse(null, "SUPER_ADMIN", "Super admin", true, true)));
        when(auditServiceClient.getMetrics()).thenReturn(new AuditMetricsResponse(42, 3));

        DashboardResponse response = dashboardService.getDashboardSummary();

        assertThat(response.users()).isEqualTo(new DashboardResponse.UserMetrics(10, 8, 1, 1));
        assertThat(response.organizations()).isEqualTo(new DashboardResponse.OrganizationMetrics(4, 3, 0));
        assertThat(response.roles()).isEqualTo(new DashboardResponse.RoleMetrics(2));
        assertThat(response.audit()).isEqualTo(new DashboardResponse.AuditMetrics(42, 3));
    }
}
