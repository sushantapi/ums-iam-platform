package com.ums.hrms.employee.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "hrms_employees",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_employee_org_code", columnNames = {"organization_id", "employee_code"}),
                @UniqueConstraint(name = "uk_employee_org_ums_user", columnNames = {"organization_id", "ums_user_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ums_user_id", nullable = false, length = 36)
    private UUID umsUserId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "organization_id", nullable = false, length = 36)
    private UUID organizationId;

    @Column(name = "employee_code", nullable = false, length = 64)
    private String employeeCode;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "department_id", length = 36)
    private UUID departmentId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "designation_id", length = 36)
    private UUID designationId;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "date_of_joining")
    private LocalDate dateOfJoining;

    @Column(name = "pan_display", length = 64)
    private String panDisplay;

    @Column(name = "uan_display", length = 64)
    private String uanDisplay;

    @Column(name = "esi_display", length = 64)
    private String esiDisplay;

    @Column(name = "bank_account_display", length = 64)
    private String bankAccountDisplay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmployeeStatus status;
}
