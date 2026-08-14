package com.ums.auth.dto.admin;

public record AdminUserMetricsResponse(long total, long active, long locked, long suspended) {
}
