package com.ums.hrms.employee.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ums.hrms.employee.entity.EmployeeStatus;

public record EmployeeResponse(
        UUID id,
        UUID umsUserId,
        UUID organizationId,
        String employeeCode,
        UUID departmentId,
        UUID designationId,
        EmployeeStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
