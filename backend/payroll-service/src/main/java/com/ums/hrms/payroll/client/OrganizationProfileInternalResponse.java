package com.ums.hrms.payroll.client;

import java.util.UUID;

public record OrganizationProfileInternalResponse(
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
        Integer logoAssetVersion) {
}
