package com.ums.hrms.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.ums.hrms.payroll.entity.TaxRegime;

public record SalaryStructureResponse(
        UUID id,
        UUID organizationId,
        UUID employeeId,
        String currency,
        BigDecimal basicPay,
        BigDecimal allowanceTotal,
        BigDecimal deductionTotal,
        boolean pfApplicable,
        BigDecimal pfContributionWage,
        boolean esiApplicable,
        BigDecimal esiContributionWage,
        BigDecimal tdsAmount,
        TaxRegime taxRegime,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean active,
        UUID createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
