package com.ums.hrms.payroll.service;

import java.math.BigDecimal;
import java.util.UUID;

import com.ums.hrms.payroll.entity.TaxRegime;

public record StatutoryPayrollCalculation(
        UUID statutoryPolicyId,
        String statutoryPolicyVersion,
        BigDecimal pfContributionWage,
        BigDecimal employeePfContribution,
        BigDecimal employerPfContribution,
        BigDecimal esiContributionWage,
        BigDecimal employeeEsiContribution,
        BigDecimal employerEsiContribution,
        BigDecimal tdsAmount,
        BigDecimal statutoryEmployeeDeductionTotal,
        BigDecimal employerStatutoryContributionTotal,
        TaxRegime taxRegime) {
}
