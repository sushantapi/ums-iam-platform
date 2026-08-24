package com.ums.hrms.employee.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
        @NotNull UUID organizationId,
        @NotNull UUID umsUserId,
        @NotBlank @Size(max = 64) String employeeCode,
        UUID departmentId,
        UUID designationId,
        @Size(max = 255) String displayName,
        LocalDate dateOfJoining,
        @Size(max = 64) String panNumber,
        @Size(max = 64) String uanNumber,
        @Size(max = 64) String esiNumber,
        @Size(max = 64) String bankAccountNumber) {

    public CreateEmployeeRequest(
            UUID organizationId,
            UUID umsUserId,
            String employeeCode,
            UUID departmentId,
            UUID designationId) {
        this(
                organizationId,
                umsUserId,
                employeeCode,
                departmentId,
                designationId,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
