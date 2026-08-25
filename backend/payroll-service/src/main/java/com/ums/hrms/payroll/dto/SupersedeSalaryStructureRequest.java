package com.ums.hrms.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.ums.hrms.payroll.entity.TaxRegime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SupersedeSalaryStructureRequest(
        @NotNull UUID organizationId,
        @NotBlank @Pattern(regexp = "(?i)[A-Z]{3}", message = "currency must be a 3-letter ISO code") String currency,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal basicPay,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal allowanceTotal,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal deductionTotal,
        Boolean pfApplicable,
        @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal pfContributionWage,
        Boolean esiApplicable,
        @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal esiContributionWage,
        @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal tdsAmount,
        TaxRegime taxRegime,
        @NotNull LocalDate effectiveFrom) {
}
