package com.ums.hrms.payroll.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.ums.hrms.payroll.entity.SalaryStructure;
import com.ums.hrms.payroll.entity.StatutoryPolicy;

@Component
public class StatutoryPayrollCalculator {

    private static final BigDecimal ZERO_MONEY =
            BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    public StatutoryPayrollCalculation calculate(
            SalaryStructure structure,
            StatutoryPolicy policy) {
        boolean pfApplicable = structure.isPfApplicable();
        boolean esiApplicable = structure.isEsiApplicable();

        if ((pfApplicable || esiApplicable) && policy == null) {
            throw new IllegalArgumentException(
                    "Statutory policy is required when PF or ESI is applicable");
        }

        BigDecimal pfContributionWage = ZERO_MONEY;
        BigDecimal employeePfContribution = ZERO_MONEY;
        BigDecimal employerPfContribution = ZERO_MONEY;

        if (pfApplicable) {
            BigDecimal configuredPfWage = money(structure.getPfContributionWage());
            BigDecimal pfCeiling = money(policy.getPfContributionWageCeiling());

            pfContributionWage = configuredPfWage.min(pfCeiling);
            employeePfContribution =
                    contribution(pfContributionWage, policy.getPfEmployeeRate());
            employerPfContribution =
                    contribution(pfContributionWage, policy.getPfEmployerRate());
        }

        BigDecimal esiContributionWage = ZERO_MONEY;
        BigDecimal employeeEsiContribution = ZERO_MONEY;
        BigDecimal employerEsiContribution = ZERO_MONEY;

        if (esiApplicable) {
            esiContributionWage = money(structure.getEsiContributionWage());
            BigDecimal esiEligibilityCeiling =
                    money(policy.getEsiWageEligibilityCeiling());

            if (esiContributionWage.compareTo(esiEligibilityCeiling) <= 0) {
                employeeEsiContribution =
                        contribution(esiContributionWage, policy.getEsiEmployeeRate());
                employerEsiContribution =
                        contribution(esiContributionWage, policy.getEsiEmployerRate());
            }
        }

        BigDecimal tdsAmount = moneyOrZero(structure.getTdsAmount());

        BigDecimal statutoryEmployeeDeductionTotal = money(
                employeePfContribution
                        .add(employeeEsiContribution)
                        .add(tdsAmount));

        BigDecimal employerStatutoryContributionTotal = money(
                employerPfContribution
                        .add(employerEsiContribution));

        return new StatutoryPayrollCalculation(
                policy == null ? null : policy.getId(),
                policy == null ? null : policy.getPolicyVersion(),
                pfContributionWage,
                employeePfContribution,
                employerPfContribution,
                esiContributionWage,
                employeeEsiContribution,
                employerEsiContribution,
                tdsAmount,
                statutoryEmployeeDeductionTotal,
                employerStatutoryContributionTotal,
                structure.getTaxRegime());
    }

    private BigDecimal contribution(BigDecimal wage, BigDecimal rate) {
        return wage.multiply(rate)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal moneyOrZero(BigDecimal value) {
        return value == null ? ZERO_MONEY : money(value);
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Money value cannot be null");
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
