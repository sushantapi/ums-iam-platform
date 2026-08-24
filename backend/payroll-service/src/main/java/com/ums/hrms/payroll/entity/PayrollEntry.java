package com.ums.hrms.payroll.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Column(name = "organization_legal_name", length = 255, updatable = false)
    private String organizationLegalName;

    @Column(name = "organization_display_name", length = 255, updatable = false)
    private String organizationDisplayName;

    @Column(name = "organization_registered_address", length = 1000, updatable = false)
    private String organizationRegisteredAddress;

    @Column(name = "organization_business_email", length = 255, updatable = false)
    private String organizationBusinessEmail;

    @Column(name = "organization_business_phone", length = 50, updatable = false)
    private String organizationBusinessPhone;

    @Column(name = "organization_website", length = 255, updatable = false)
    private String organizationWebsite;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "organization_default_currency", length = 3, updatable = false)
    private String organizationDefaultCurrency;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "organization_payroll_country", length = 2, updatable = false)
    private String organizationPayrollCountry;

    @Column(name = "payslip_footer_text_snapshot", length = 500, updatable = false)
    private String payslipFooterTextSnapshot;

    @Column(name = "authorized_signatory_label_snapshot", length = 255, updatable = false)
    private String authorizedSignatoryLabelSnapshot;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "organization_logo_asset_id", length = 36, updatable = false)
    private UUID organizationLogoAssetId;

    @Column(name = "organization_logo_asset_version", updatable = false)
    private Integer organizationLogoAssetVersion;

    @Column(name = "employee_code_snapshot", length = 64, updatable = false)
    private String employeeCodeSnapshot;

    @Column(name = "employee_display_name", length = 255, updatable = false)
    private String employeeDisplayName;

    @Column(name = "employee_date_of_joining", updatable = false)
    private LocalDate employeeDateOfJoining;

    @Column(name = "employee_department_name", length = 120, updatable = false)
    private String employeeDepartmentName;

    @Column(name = "employee_designation_name", length = 120, updatable = false)
    private String employeeDesignationName;

    @Column(name = "employee_pan_display", length = 64, updatable = false)
    private String employeePanDisplay;

    @Column(name = "employee_uan_display", length = 64, updatable = false)
    private String employeeUanDisplay;

    @Column(name = "employee_esi_display", length = 64, updatable = false)
    private String employeeEsiDisplay;

    @Column(name = "employee_bank_account_display", length = 64, updatable = false)
    private String employeeBankAccountDisplay;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;
}
