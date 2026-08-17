package com.ums.hrms.employee.dto;

import java.util.List;

public record DepartmentPageResponse(
        List<DepartmentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
