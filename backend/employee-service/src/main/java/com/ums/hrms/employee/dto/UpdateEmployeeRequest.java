package com.ums.hrms.employee.dto;

import java.util.UUID;

import com.ums.hrms.employee.entity.EmployeeStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateEmployeeRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 64) String employeeCode,
        UUID departmentId,
        UUID designationId,
        @NotNull EmployeeStatus status) {
}
