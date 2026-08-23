CREATE TABLE hrms_statutory_policies (
    id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    country_code CHAR(2) NOT NULL,
    policy_version VARCHAR(50) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    pf_employee_rate DECIMAL(9,6) NOT NULL,
    pf_employer_rate DECIMAL(9,6) NOT NULL,
    pf_contribution_wage_ceiling DECIMAL(19,2) NOT NULL,
    esi_employee_rate DECIMAL(9,6) NOT NULL,
    esi_employer_rate DECIMAL(9,6) NOT NULL,
    esi_wage_eligibility_ceiling DECIMAL(19,2) NOT NULL,
    created_by CHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_statutory_policy_org_country_version
        UNIQUE (organization_id, country_code, policy_version),
    CONSTRAINT chk_statutory_policy_dates CHECK (
        effective_to IS NULL OR effective_to >= effective_from
    ),
    CONSTRAINT chk_statutory_policy_rates CHECK (
        pf_employee_rate >= 0 AND pf_employee_rate <= 1
        AND pf_employer_rate >= 0 AND pf_employer_rate <= 1
        AND esi_employee_rate >= 0 AND esi_employee_rate <= 1
        AND esi_employer_rate >= 0 AND esi_employer_rate <= 1
    ),
    CONSTRAINT chk_statutory_policy_ceilings CHECK (
        pf_contribution_wage_ceiling >= 0
        AND esi_wage_eligibility_ceiling >= 0
    )
);

CREATE INDEX idx_statutory_policy_org_country_effective
    ON hrms_statutory_policies (
        organization_id,
        country_code,
        active,
        effective_from,
        effective_to
    );

ALTER TABLE hrms_salary_structures
    ADD COLUMN pf_applicable BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE hrms_salary_structures
    ADD COLUMN pf_contribution_wage DECIMAL(19,2) NULL;

ALTER TABLE hrms_salary_structures
    ADD COLUMN esi_applicable BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE hrms_salary_structures
    ADD COLUMN esi_contribution_wage DECIMAL(19,2) NULL;

ALTER TABLE hrms_salary_structures
    ADD COLUMN tds_amount DECIMAL(19,2) NOT NULL DEFAULT 0.00;

ALTER TABLE hrms_salary_structures
    ADD COLUMN tax_regime VARCHAR(10) NULL;

ALTER TABLE hrms_salary_structures
    ADD CONSTRAINT chk_salary_structure_statutory_inputs CHECK (
        (pf_contribution_wage IS NULL OR pf_contribution_wage >= 0)
        AND (esi_contribution_wage IS NULL OR esi_contribution_wage >= 0)
        AND tds_amount >= 0
        AND (pf_applicable = FALSE OR pf_contribution_wage IS NOT NULL)
        AND (esi_applicable = FALSE OR esi_contribution_wage IS NOT NULL)
        AND (tax_regime IS NULL OR tax_regime IN ('OLD', 'NEW'))
    );

ALTER TABLE hrms_payroll_entries
    ADD COLUMN statutory_policy_id CHAR(36) NULL;

ALTER TABLE hrms_payroll_entries
    ADD COLUMN statutory_policy_version VARCHAR(50) NULL;

ALTER TABLE hrms_payroll_entries
    ADD COLUMN configured_deduction_total DECIMAL(19,2) NOT NULL DEFAULT 0.00;

ALTER TABLE hrms_payroll_entries
    ADD COLUMN pf_contribution_wage DECIMAL(19,2) NOT NULL DEFAULT 0.00;

ALTER TABLE hrms_payroll_entries
    ADD COLUMN employee_pf_contribution DECIMAL(19,2) NOT NULL DEFAULT 0.00;

ALTER TABLE hrms_payroll_entries
    ADD COLUMN employer_pf_contribution DECIMAL(19,2) NOT NULL DEFAULT 0.00;

ALTER TABLE hrms_payroll_entries
    ADD COLUMN esi_contribution_wage DECIMAL(19,2) NOT NULL DEFAULT 0.00;

ALTER TABLE hrms_payroll_entries
    ADD COLUMN employee_esi_contribution DECIMAL(19,2) NOT NULL DEFAULT 0.00;

ALTER TABLE hrms_payroll_entries
    ADD COLUMN employer_esi_contribution DECIMAL(19,2) NOT NULL DEFAULT 0.00;

ALTER TABLE hrms_payroll_entries
    ADD COLUMN tds_amount DECIMAL(19,2) NOT NULL DEFAULT 0.00;

ALTER TABLE hrms_payroll_entries
    ADD COLUMN statutory_employee_deduction_total DECIMAL(19,2) NOT NULL DEFAULT 0.00;

ALTER TABLE hrms_payroll_entries
    ADD COLUMN employer_statutory_contribution_total DECIMAL(19,2) NOT NULL DEFAULT 0.00;

ALTER TABLE hrms_payroll_entries
    ADD COLUMN tax_regime VARCHAR(10) NULL;

UPDATE hrms_payroll_entries
SET configured_deduction_total = deduction_total;

ALTER TABLE hrms_payroll_entries
    ADD CONSTRAINT fk_payroll_entry_statutory_policy
        FOREIGN KEY (statutory_policy_id)
        REFERENCES hrms_statutory_policies(id);

ALTER TABLE hrms_payroll_entries
    ADD CONSTRAINT chk_payroll_entry_statutory_money CHECK (
        configured_deduction_total >= 0
        AND pf_contribution_wage >= 0
        AND employee_pf_contribution >= 0
        AND employer_pf_contribution >= 0
        AND esi_contribution_wage >= 0
        AND employee_esi_contribution >= 0
        AND employer_esi_contribution >= 0
        AND tds_amount >= 0
        AND statutory_employee_deduction_total >= 0
        AND employer_statutory_contribution_total >= 0
        AND (tax_regime IS NULL OR tax_regime IN ('OLD', 'NEW'))
    );

CREATE INDEX idx_payroll_entry_statutory_policy
    ON hrms_payroll_entries (statutory_policy_id);
