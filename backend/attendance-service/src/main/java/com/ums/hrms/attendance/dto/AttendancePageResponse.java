package com.ums.hrms.attendance.dto;

import java.util.List;

public record AttendancePageResponse(
        List<AttendanceResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
