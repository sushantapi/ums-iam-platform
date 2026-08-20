package com.ums.hrms.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SalaryStructureResponse(
        UUID id,
        UUID organizationId,
        UUID employeeId,
        String currency,
        BigDecimal basicPay,
        BigDecimal allowanceTotal,
        BigDecimal deductionTotal,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean active,
        UUID createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
