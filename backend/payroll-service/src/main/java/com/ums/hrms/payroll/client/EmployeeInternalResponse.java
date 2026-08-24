package com.ums.hrms.payroll.client;

import java.time.LocalDate;
import java.util.UUID;

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
        String status) {

    public EmployeeInternalResponse(
            UUID id,
            UUID organizationId,
            String employeeCode,
            String status) {
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
