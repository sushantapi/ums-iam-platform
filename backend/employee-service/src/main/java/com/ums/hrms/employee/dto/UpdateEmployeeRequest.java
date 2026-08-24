package com.ums.hrms.employee.dto;

import java.time.LocalDate;
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
        @NotNull EmployeeStatus status,
        @Size(max = 255) String displayName,
        LocalDate dateOfJoining,
        @Size(max = 64) String panNumber,
        @Size(max = 64) String uanNumber,
        @Size(max = 64) String esiNumber,
        @Size(max = 64) String bankAccountNumber) {

    public UpdateEmployeeRequest(
            UUID organizationId,
            String employeeCode,
            UUID departmentId,
            UUID designationId,
            EmployeeStatus status) {
        this(
                organizationId,
                employeeCode,
                departmentId,
                designationId,
                status,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
