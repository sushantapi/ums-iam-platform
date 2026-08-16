package com.ums.admin.dto.response;

public record DashboardResponse(
		UserMetrics users,
		OrganizationMetrics organizations,
		RoleMetrics roles,
		AuditMetrics audit) {

	public record UserMetrics(long total, long active, long locked, long suspended) {
	}

	public record OrganizationMetrics(long total, long active, long pendingInvitations) {
	}

	public record RoleMetrics(long total) {
	}

	public record AuditMetrics(long eventsLast24Hours, long failedLogins) {
	}
}
