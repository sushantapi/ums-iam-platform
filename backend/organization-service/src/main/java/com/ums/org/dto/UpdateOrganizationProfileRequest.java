package com.ums.org.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationProfileRequest(

        @Size(max = 255, message = "Legal name must not exceed 255 characters") String legalName,

        @Size(max = 255, message = "Display name must not exceed 255 characters") String displayName,

        @Size(max = 1000, message = "Registered address must not exceed 1000 characters") String registeredAddress,

        @Email(message = "Business email must be valid")
        @Size(max = 255, message = "Business email must not exceed 255 characters") String businessEmail,

        @Size(max = 50, message = "Business phone must not exceed 50 characters") String businessPhone,

        @Size(max = 255, message = "Website must not exceed 255 characters") String website,

        @Pattern(regexp = "(?i)[A-Z]{3}", message = "defaultCurrency must be a 3-letter ISO code") String defaultCurrency,

        @Pattern(regexp = "(?i)[A-Z]{2}", message = "payrollCountry must be a 2-letter country code") String payrollCountry,

        @Size(max = 500, message = "Payslip footer text must not exceed 500 characters") String payslipFooterText,

        @Size(max = 255, message = "Authorized signatory label must not exceed 255 characters") String authorizedSignatoryLabel

) {
}
