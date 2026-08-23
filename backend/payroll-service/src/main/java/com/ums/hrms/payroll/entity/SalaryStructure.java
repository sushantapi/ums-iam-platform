package com.ums.hrms.payroll.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hrms_salary_structures")
@Getter
@Setter
@NoArgsConstructor
public class SalaryStructure extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "organization_id", nullable = false, length = 36)
    private UUID organizationId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "employee_id", nullable = false, length = 36)
    private UUID employeeId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "basic_pay", nullable = false, precision = 19, scale = 2)
    private BigDecimal basicPay;

    @Column(name = "allowance_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal allowanceTotal;

    @Column(name = "deduction_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal deductionTotal;

    @Column(name = "pf_applicable", nullable = false)
    private boolean pfApplicable;

    @Column(name = "pf_contribution_wage", precision = 19, scale = 2)
    private BigDecimal pfContributionWage;

    @Column(name = "esi_applicable", nullable = false)
    private boolean esiApplicable;

    @Column(name = "esi_contribution_wage", precision = 19, scale = 2)
    private BigDecimal esiContributionWage;

    @Column(name = "tds_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal tdsAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_regime", length = 10)
    private TaxRegime taxRegime;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false)
    private boolean active = true;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "created_by", nullable = false, length = 36, updatable = false)
    private UUID createdBy;
}
