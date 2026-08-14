package com.ums.dto;

public record AuditMetricsResponse(long eventsLast24Hours, long failedLogins) {
}
