package com.ums.admin.dto.response;

public record AuditMetricsResponse(long eventsLast24Hours, long failedLogins) {
}
