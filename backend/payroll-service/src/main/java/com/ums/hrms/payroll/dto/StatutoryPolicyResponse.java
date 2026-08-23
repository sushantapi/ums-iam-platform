package com.ums.hrms.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record StatutoryPolicyResponse(
        UUID id,
        UUID organizationId,
        String countryCode,
        String policyVersion,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean active,
        BigDecimal pfEmployeeRate,
        BigDecimal pfEmployerRate,
        BigDecimal pfContributionWageCeiling,
        BigDecimal esiEmployeeRate,
        BigDecimal esiEmployerRate,
        BigDecimal esiWageEligibilityCeiling,
        UUID createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}