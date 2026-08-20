package com.ums.hrms.payroll.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;
}
