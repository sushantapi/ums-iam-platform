package com.ums.hrms.payroll.entity;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
        name = "hrms_payroll_runs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payroll_run_org_month",
                columnNames = {"organization_id", "payroll_month"}))
@Getter
@Setter
@NoArgsConstructor
public class PayrollRun extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "organization_id", nullable = false, length = 36)
    private UUID organizationId;

    @Convert(converter = YearMonthAttributeConverter.class)
    @Column(name = "payroll_month", nullable = false, length = 7)
    private YearMonth payrollMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayrollRunStatus status = PayrollRunStatus.DRAFT;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "created_by", nullable = false, length = 36, updatable = false)
    private UUID createdBy;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "processed_by", length = 36)
    private UUID processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "finalized_by", length = 36)
    private UUID finalizedBy;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;
}
