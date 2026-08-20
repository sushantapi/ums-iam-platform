package com.ums.hrms.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PayrollEntryResponse(
        UUID id,
        UUID payrollRunId,
        UUID organizationId,
        UUID employeeId,
        UUID salaryStructureId,
        BigDecimal basicPay,
        BigDecimal allowanceTotal,
        BigDecimal grossPay,
        BigDecimal deductionTotal,
        BigDecimal netPay,
        LocalDateTime generatedAt) {
}
