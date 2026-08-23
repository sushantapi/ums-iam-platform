package com.ums.hrms.payroll.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "hrms_statutory_policies",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_statutory_policy_org_country_version",
                columnNames = {"organization_id", "country_code", "policy_version"}))
@Getter
@Setter
@NoArgsConstructor
public class StatutoryPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "organization_id", nullable = false, length = 36)
    private UUID organizationId;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "policy_version", nullable = false, length = 50)
    private String policyVersion;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "pf_employee_rate", nullable = false, precision = 9, scale = 6)
    private BigDecimal pfEmployeeRate;

    @Column(name = "pf_employer_rate", nullable = false, precision = 9, scale = 6)
    private BigDecimal pfEmployerRate;

    @Column(name = "pf_contribution_wage_ceiling", nullable = false, precision = 19, scale = 2)
    private BigDecimal pfContributionWageCeiling;

    @Column(name = "esi_employee_rate", nullable = false, precision = 9, scale = 6)
    private BigDecimal esiEmployeeRate;

    @Column(name = "esi_employer_rate", nullable = false, precision = 9, scale = 6)
    private BigDecimal esiEmployerRate;

    @Column(name = "esi_wage_eligibility_ceiling", nullable = false, precision = 19, scale = 2)
    private BigDecimal esiWageEligibilityCeiling;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "created_by", nullable = false, length = 36, updatable = false)
    private UUID createdBy;
}
