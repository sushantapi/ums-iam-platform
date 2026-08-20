package com.ums.hrms.leave.dto;

import java.util.List;

public record LeavePageResponse(
        List<LeaveResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
