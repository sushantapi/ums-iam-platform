package com.ums.org.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizationProfileResponse(
        UUID organizationId,
        String legalName,
        String displayName,
        String registeredAddress,
        String businessEmail,
        String businessPhone,
        String website,
        String defaultCurrency,
        String payrollCountry,
        String payslipFooterText,
        String authorizedSignatoryLabel,
        UUID logoAssetId,
        Integer logoAssetVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
