package com.ums.hrms.employee.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.ums.hrms.employee.entity.EmployeeStatus;

public record EmployeeInternalResponse(
        UUID id,
        UUID organizationId,
        String employeeCode,
        String displayName,
        LocalDate dateOfJoining,
        String departmentName,
        String designationName,
        String panDisplay,
        String uanDisplay,
        String esiDisplay,
        String bankAccountDisplay,
        EmployeeStatus status) {

    public EmployeeInternalResponse(
            UUID id,
            UUID organizationId,
            String employeeCode,
            EmployeeStatus status) {
        this(
                id,
                organizationId,
                employeeCode,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                status);
    }
}
