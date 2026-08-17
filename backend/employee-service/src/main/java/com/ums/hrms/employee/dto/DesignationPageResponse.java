package com.ums.hrms.employee.dto;

import java.util.List;

public record DesignationPageResponse(
        List<DesignationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
