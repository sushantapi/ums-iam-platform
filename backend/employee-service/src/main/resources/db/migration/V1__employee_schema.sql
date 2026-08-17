CREATE TABLE hrms_employees (
    id CHAR(36) PRIMARY KEY,
    ums_user_id CHAR(36) NOT NULL,
    organization_id CHAR(36) NOT NULL,
    employee_code VARCHAR(64) NOT NULL,
    department_id CHAR(36),
    designation_id CHAR(36),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX uk_employee_org_code
ON hrms_employees(organization_id, employee_code);

CREATE UNIQUE INDEX uk_employee_org_ums_user
ON hrms_employees(organization_id, ums_user_id);

CREATE INDEX idx_employee_org
ON hrms_employees(organization_id);
