package com.ums.hrms.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.ums.hrms.payroll.entity.TaxRegime;

public record CreateSalaryStructureRequest(
        @NotNull UUID organizationId,
        @NotNull UUID employeeId,
        @Pattern(regexp = "(?i)[A-Z]{3}", message = "currency must be a 3-letter ISO code") String currency,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal basicPay,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal allowanceTotal,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal deductionTotal,
        Boolean pfApplicable,
        @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal pfContributionWage,
        Boolean esiApplicable,
        @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal esiContributionWage,
        @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal tdsAmount,
        TaxRegime taxRegime,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Boolean active) {

    public CreateSalaryStructureRequest(
            UUID organizationId,
            UUID employeeId,
            String currency,
            BigDecimal basicPay,
            BigDecimal allowanceTotal,
            BigDecimal deductionTotal,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            Boolean active) {
        this(
                organizationId,
                employeeId,
                currency,
                basicPay,
                allowanceTotal,
                deductionTotal,
                null,
                null,
                null,
                null,
                null,
                null,
                effectiveFrom,
                effectiveTo,
                active);
    }
}
