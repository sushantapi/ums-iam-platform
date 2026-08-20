CREATE TABLE hrms_salary_structures (
    id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    employee_id CHAR(36) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    basic_pay DECIMAL(19,2) NOT NULL,
    allowance_total DECIMAL(19,2) NOT NULL,
    deduction_total DECIMAL(19,2) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by CHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_salary_structure_money CHECK (
        basic_pay >= 0 AND allowance_total >= 0 AND deduction_total >= 0
    ),
    CONSTRAINT chk_salary_structure_dates CHECK (
        effective_to IS NULL OR effective_to >= effective_from
    ),
    INDEX idx_salary_structure_org_employee (organization_id, employee_id),
    INDEX idx_salary_structure_org_employee_dates (organization_id, employee_id, effective_from, effective_to),
    INDEX idx_salary_structure_org_active (organization_id, active)
);

CREATE TABLE hrms_payroll_runs (
    id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    payroll_month VARCHAR(7) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_by CHAR(36) NOT NULL,
    processed_by CHAR(36) NULL,
    processed_at DATETIME(6) NULL,
    finalized_by CHAR(36) NULL,
    finalized_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payroll_run_org_month UNIQUE (organization_id, payroll_month),
    CONSTRAINT chk_payroll_run_month CHECK (payroll_month REGEXP '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    CONSTRAINT chk_payroll_run_status CHECK (status IN ('DRAFT', 'PROCESSED', 'FINALIZED')),
    INDEX idx_payroll_run_org_status (organization_id, status),
    INDEX idx_payroll_run_org_month (organization_id, payroll_month)
);

CREATE TABLE hrms_payroll_entries (
    id CHAR(36) NOT NULL,
    payroll_run_id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    employee_id CHAR(36) NOT NULL,
    salary_structure_id CHAR(36) NOT NULL,
    basic_pay DECIMAL(19,2) NOT NULL,
    allowance_total DECIMAL(19,2) NOT NULL,
    gross_pay DECIMAL(19,2) NOT NULL,
    deduction_total DECIMAL(19,2) NOT NULL,
    net_pay DECIMAL(19,2) NOT NULL,
    generated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payroll_entry_run_employee UNIQUE (payroll_run_id, employee_id),
    CONSTRAINT chk_payroll_entry_money CHECK (
        basic_pay >= 0
        AND allowance_total >= 0
        AND gross_pay >= 0
        AND deduction_total >= 0
        AND net_pay >= 0
    ),
    CONSTRAINT fk_payroll_entry_run FOREIGN KEY (payroll_run_id)
        REFERENCES hrms_payroll_runs(id),
    CONSTRAINT fk_payroll_entry_salary_structure FOREIGN KEY (salary_structure_id)
        REFERENCES hrms_salary_structures(id),
    INDEX idx_payroll_entry_org_run (organization_id, payroll_run_id),
    INDEX idx_payroll_entry_org_employee (organization_id, employee_id)
);
