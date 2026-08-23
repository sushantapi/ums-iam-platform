package com.ums.hrms.payroll.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ums.hrms.payroll.entity.SalaryStructure;
import com.ums.hrms.payroll.entity.StatutoryPolicy;
import com.ums.hrms.payroll.entity.TaxRegime;

class StatutoryPayrollCalculatorTests {

    private final StatutoryPayrollCalculator calculator =
            new StatutoryPayrollCalculator();

    @Test
    void capsPfWageAndCalculatesEmployeeAndEmployerContributions() {
        SalaryStructure structure = salaryStructure();
        structure.setPfApplicable(true);
        structure.setPfContributionWage(new BigDecimal("20000.00"));
        structure.setTdsAmount(new BigDecimal("1250.00"));
        structure.setTaxRegime(TaxRegime.NEW);

        StatutoryPolicy policy = policy();

        StatutoryPayrollCalculation result =
                calculator.calculate(structure, policy);

        assertEquals(policy.getId(), result.statutoryPolicyId());
        assertEquals("IN-2026.1", result.statutoryPolicyVersion());

        assertEquals(
                new BigDecimal("15000.00"),
                result.pfContributionWage());
        assertEquals(
                new BigDecimal("1800.00"),
                result.employeePfContribution());
        assertEquals(
                new BigDecimal("1800.00"),
                result.employerPfContribution());

        assertEquals(
                new BigDecimal("3050.00"),
                result.statutoryEmployeeDeductionTotal());
        assertEquals(
                new BigDecimal("1800.00"),
                result.employerStatutoryContributionTotal());
        assertEquals(TaxRegime.NEW, result.taxRegime());
    }

    @Test
    void calculatesEligibleEsiAndKeepsEmployerContributionSeparate() {
        SalaryStructure structure = salaryStructure();
        structure.setEsiApplicable(true);
        structure.setEsiContributionWage(new BigDecimal("18000.00"));
        structure.setTdsAmount(new BigDecimal("500.00"));

        StatutoryPayrollCalculation result =
                calculator.calculate(structure, policy());

        assertEquals(
                new BigDecimal("18000.00"),
                result.esiContributionWage());
        assertEquals(
                new BigDecimal("135.00"),
                result.employeeEsiContribution());
        assertEquals(
                new BigDecimal("585.00"),
                result.employerEsiContribution());

        assertEquals(
                new BigDecimal("635.00"),
                result.statutoryEmployeeDeductionTotal());
        assertEquals(
                new BigDecimal("585.00"),
                result.employerStatutoryContributionTotal());
    }

    @Test
    void preservesConfiguredEsiWageButProducesZeroWhenAboveEligibilityCeiling() {
        SalaryStructure structure = salaryStructure();
        structure.setEsiApplicable(true);
        structure.setEsiContributionWage(new BigDecimal("22000.00"));

        StatutoryPayrollCalculation result =
                calculator.calculate(structure, policy());

        assertEquals(
                new BigDecimal("22000.00"),
                result.esiContributionWage());
        assertEquals(
                new BigDecimal("0.00"),
                result.employeeEsiContribution());
        assertEquals(
                new BigDecimal("0.00"),
                result.employerEsiContribution());
        assertEquals(
                new BigDecimal("0.00"),
                result.statutoryEmployeeDeductionTotal());
        assertEquals(
                new BigDecimal("0.00"),
                result.employerStatutoryContributionTotal());
    }

    @Test
    void supportsTdsOnlyWithoutStatutoryPolicy() {
        SalaryStructure structure = salaryStructure();
        structure.setTdsAmount(new BigDecimal("1250.00"));
        structure.setTaxRegime(TaxRegime.OLD);

        StatutoryPayrollCalculation result =
                calculator.calculate(structure, null);

        assertNull(result.statutoryPolicyId());
        assertNull(result.statutoryPolicyVersion());

        assertEquals(
                new BigDecimal("0.00"),
                result.pfContributionWage());
        assertEquals(
                new BigDecimal("0.00"),
                result.esiContributionWage());
        assertEquals(
                new BigDecimal("1250.00"),
                result.tdsAmount());
        assertEquals(
                new BigDecimal("1250.00"),
                result.statutoryEmployeeDeductionTotal());
        assertEquals(
                new BigDecimal("0.00"),
                result.employerStatutoryContributionTotal());
        assertEquals(TaxRegime.OLD, result.taxRegime());
    }

    @Test
    void roundsContributionsHalfUpToTwoDecimals() {
        SalaryStructure structure = salaryStructure();
        structure.setEsiApplicable(true);
        structure.setEsiContributionWage(new BigDecimal("1000.67"));

        StatutoryPayrollCalculation result =
                calculator.calculate(structure, policy());

        assertEquals(
                new BigDecimal("7.51"),
                result.employeeEsiContribution());
        assertEquals(
                new BigDecimal("32.52"),
                result.employerEsiContribution());
    }

    @Test
    void requiresPolicyWhenPfOrEsiIsApplicable() {
        SalaryStructure structure = salaryStructure();
        structure.setPfApplicable(true);
        structure.setPfContributionWage(new BigDecimal("15000.00"));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(structure, null));

        assertEquals(
                "Statutory policy is required when PF or ESI is applicable",
                ex.getMessage());
    }

    private SalaryStructure salaryStructure() {
        SalaryStructure structure = new SalaryStructure();
        structure.setId(UUID.randomUUID());
        structure.setOrganizationId(UUID.randomUUID());
        structure.setEmployeeId(UUID.randomUUID());
        structure.setBasicPay(new BigDecimal("50000.00"));
        structure.setAllowanceTotal(new BigDecimal("10000.00"));
        structure.setDeductionTotal(new BigDecimal("5000.00"));
        structure.setTdsAmount(BigDecimal.ZERO);
        return structure;
    }

    private StatutoryPolicy policy() {
        StatutoryPolicy policy = new StatutoryPolicy();
        policy.setId(UUID.randomUUID());
        policy.setOrganizationId(UUID.randomUUID());
        policy.setCountryCode("IN");
        policy.setPolicyVersion("IN-2026.1");

        policy.setPfEmployeeRate(new BigDecimal("0.120000"));
        policy.setPfEmployerRate(new BigDecimal("0.120000"));
        policy.setPfContributionWageCeiling(new BigDecimal("15000.00"));

        policy.setEsiEmployeeRate(new BigDecimal("0.007500"));
        policy.setEsiEmployerRate(new BigDecimal("0.032500"));
        policy.setEsiWageEligibilityCeiling(new BigDecimal("21000.00"));

        return policy;
    }
}
