package com.ums.hrms.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStatutoryPolicyRequest(
        @NotNull UUID organizationId,
        @NotBlank
        @Pattern(regexp = "(?i)[A-Z]{2}", message = "countryCode must be a 2-letter country code")
        String countryCode,
        @NotBlank @Size(max = 50) String policyVersion,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo,
        Boolean active,
        @NotNull
        @DecimalMin("0.000000")
        @DecimalMax("1.000000")
        @Digits(integer = 1, fraction = 6)
        BigDecimal pfEmployeeRate,
        @NotNull
        @DecimalMin("0.000000")
        @DecimalMax("1.000000")
        @Digits(integer = 1, fraction = 6)
        BigDecimal pfEmployerRate,
        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 17, fraction = 2)
        BigDecimal pfContributionWageCeiling,
        @NotNull
        @DecimalMin("0.000000")
        @DecimalMax("1.000000")
        @Digits(integer = 1, fraction = 6)
        BigDecimal esiEmployeeRate,
        @NotNull
        @DecimalMin("0.000000")
        @DecimalMax("1.000000")
        @Digits(integer = 1, fraction = 6)
        BigDecimal esiEmployerRate,
        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 17, fraction = 2)
        BigDecimal esiWageEligibilityCeiling) {
}