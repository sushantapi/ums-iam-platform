package com.ums.admin.service.impl;

import org.springframework.stereotype.Service;

import com.ums.admin.client.AuditServiceClient;
import com.ums.admin.client.AuthenticationServiceClient;
import com.ums.admin.client.OrganizationServiceClient;
import com.ums.admin.client.RoleServiceClient;
import com.ums.admin.dto.response.AuditMetricsResponse;
import com.ums.admin.dto.response.DashboardResponse;
import com.ums.admin.dto.response.OrganizationMetricsResponse;
import com.ums.admin.dto.response.UserMetricsResponse;
import com.ums.admin.service.DashboardService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AuthenticationServiceClient authenticationServiceClient;
    private final OrganizationServiceClient organizationServiceClient;
    private final RoleServiceClient roleServiceClient;
    private final AuditServiceClient auditServiceClient;

    @Override
    public DashboardResponse getDashboardSummary() {
        UserMetricsResponse userMetrics = authenticationServiceClient.getMetrics();
        OrganizationMetricsResponse organizationMetrics = organizationServiceClient.getMetrics();
        AuditMetricsResponse auditMetrics = auditServiceClient.getMetrics();
        long totalRoles = roleServiceClient.getRoles().size();

        return new DashboardResponse(
                new DashboardResponse.UserMetrics(
                        userMetrics.total(), userMetrics.active(), userMetrics.locked(), userMetrics.suspended()),
                new DashboardResponse.OrganizationMetrics(
                        organizationMetrics.total(), organizationMetrics.active(), organizationMetrics.pendingInvitations()),
                new DashboardResponse.RoleMetrics(totalRoles),
                new DashboardResponse.AuditMetrics(
                        auditMetrics.eventsLast24Hours(), auditMetrics.failedLogins()));
    }
}
