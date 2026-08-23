package com.ums.hrms.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.ums.hrms.payroll.entity.TaxRegime;

public record PayrollEntryResponse(
        UUID id,
        UUID payrollRunId,
        UUID organizationId,
        UUID employeeId,
        UUID salaryStructureId,
        BigDecimal basicPay,
        BigDecimal allowanceTotal,
        BigDecimal grossPay,
        BigDecimal configuredDeductionTotal,
        BigDecimal pfContributionWage,
        BigDecimal employeePfContribution,
        BigDecimal employerPfContribution,
        BigDecimal esiContributionWage,
        BigDecimal employeeEsiContribution,
        BigDecimal employerEsiContribution,
        BigDecimal tdsAmount,
        BigDecimal statutoryEmployeeDeductionTotal,
        BigDecimal employerStatutoryContributionTotal,
        UUID statutoryPolicyId,
        String statutoryPolicyVersion,
        TaxRegime taxRegime,
        BigDecimal deductionTotal,
        BigDecimal netPay,
        LocalDateTime generatedAt) {

    public PayrollEntryResponse(
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
        this(
                id,
                payrollRunId,
                organizationId,
                employeeId,
                salaryStructureId,
                basicPay,
                allowanceTotal,
                grossPay,
                deductionTotal,
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2),
                null,
                null,
                null,
                deductionTotal,
                netPay,
                generatedAt);
    }
}
