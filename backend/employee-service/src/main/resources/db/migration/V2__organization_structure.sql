CREATE TABLE hrms_departments (
    id CHAR(36) PRIMARY KEY,
    organization_id CHAR(36) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX uk_department_org_code
ON hrms_departments(organization_id, code);

CREATE INDEX idx_department_org_status
ON hrms_departments(organization_id, status);

CREATE TABLE hrms_designations (
    id CHAR(36) PRIMARY KEY,
    organization_id CHAR(36) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX uk_designation_org_code
ON hrms_designations(organization_id, code);

CREATE INDEX idx_designation_org_status
ON hrms_designations(organization_id, status);
