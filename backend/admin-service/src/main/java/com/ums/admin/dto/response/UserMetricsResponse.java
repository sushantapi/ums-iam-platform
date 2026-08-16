package com.ums.admin.dto.response;

public record UserMetricsResponse(long total, long active, long locked, long suspended) {
}
