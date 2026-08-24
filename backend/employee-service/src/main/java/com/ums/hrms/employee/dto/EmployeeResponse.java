package com.ums.hrms.employee.dto;

import java.time.LocalDate;
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
        String displayName,
        LocalDate dateOfJoining,
        String panDisplay,
        String uanDisplay,
        String esiDisplay,
        String bankAccountDisplay,
        EmployeeStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
