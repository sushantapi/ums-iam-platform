package com.ums.hrms.employee.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
        @NotNull UUID organizationId,
        @NotNull UUID umsUserId,
        @NotBlank @Size(max = 64) String employeeCode,
        UUID departmentId,
        UUID designationId) {
}
