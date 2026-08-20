package com.ums.hrms.payroll.dto;

import java.time.YearMonth;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreatePayrollRunRequest(
        @NotNull UUID organizationId,
        @NotNull YearMonth payrollMonth) {
}
