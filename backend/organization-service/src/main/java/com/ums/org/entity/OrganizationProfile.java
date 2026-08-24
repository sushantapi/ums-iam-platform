package com.ums.org.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "organization_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationProfile extends BaseEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "organization_id", length = 36)
    private UUID organizationId;

    @Column(name = "legal_name", length = 255)
    private String legalName;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "registered_address", length = 1000)
    private String registeredAddress;

    @Column(name = "business_email", length = 255)
    private String businessEmail;

    @Column(name = "business_phone", length = 50)
    private String businessPhone;

    @Column(name = "website", length = 255)
    private String website;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "default_currency", length = 3)
    private String defaultCurrency;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "payroll_country", length = 2)
    private String payrollCountry;

    @Column(name = "payslip_footer_text", length = 500)
    private String payslipFooterText;

    @Column(name = "authorized_signatory_label", length = 255)
    private String authorizedSignatoryLabel;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "logo_asset_id", length = 36)
    private UUID logoAssetId;

    @Column(name = "logo_asset_version")
    private Integer logoAssetVersion;
}
