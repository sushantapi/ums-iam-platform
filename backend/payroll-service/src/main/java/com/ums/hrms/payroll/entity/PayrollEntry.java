package com.ums.hrms.payroll.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "hrms_payroll_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payroll_entry_run_employee",
                columnNames = {"payroll_run_id", "employee_id"}))
@Getter
@Setter
@NoArgsConstructor
public class PayrollEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "payroll_run_id", nullable = false, length = 36, updatable = false)
    private UUID payrollRunId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "organization_id", nullable = false, length = 36, updatable = false)
    private UUID organizationId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "employee_id", nullable = false, length = 36, updatable = false)
    private UUID employeeId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "salary_structure_id", nullable = false, length = 36, updatable = false)
    private UUID salaryStructureId;

    @Column(name = "basic_pay", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal basicPay;

    @Column(name = "allowance_total", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal allowanceTotal;

    @Column(name = "gross_pay", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal grossPay;

    @Column(name = "deduction_total", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal deductionTotal;

    @Column(name = "net_pay", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal netPay;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "statutory_policy_id", length = 36, updatable = false)
    private UUID statutoryPolicyId;

    @Column(name = "statutory_policy_version", length = 50, updatable = false)
    private String statutoryPolicyVersion;

    @Column(name = "configured_deduction_total", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal configuredDeductionTotal = BigDecimal.ZERO;

    @Column(name = "pf_contribution_wage", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal pfContributionWage = BigDecimal.ZERO;

    @Column(name = "employee_pf_contribution", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal employeePfContribution = BigDecimal.ZERO;

    @Column(name = "employer_pf_contribution", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal employerPfContribution = BigDecimal.ZERO;

    @Column(name = "esi_contribution_wage", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal esiContributionWage = BigDecimal.ZERO;

    @Column(name = "employee_esi_contribution", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal employeeEsiContribution = BigDecimal.ZERO;

    @Column(name = "employer_esi_contribution", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal employerEsiContribution = BigDecimal.ZERO;

    @Column(name = "tds_amount", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal tdsAmount = BigDecimal.ZERO;

    @Column(name = "statutory_employee_deduction_total", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal statutoryEmployeeDeductionTotal = BigDecimal.ZERO;

    @Column(name = "employer_statutory_contribution_total", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal employerStatutoryContributionTotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_regime", length = 10, updatable = false)
    private TaxRegime taxRegime;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;
}
