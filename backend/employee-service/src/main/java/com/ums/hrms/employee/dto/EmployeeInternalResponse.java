package com.ums.hrms.employee.dto;

import java.util.UUID;

import com.ums.hrms.employee.entity.EmployeeStatus;

public record EmployeeInternalResponse(
        UUID id,
        UUID organizationId,
        String employeeCode,
        EmployeeStatus status) {
}
