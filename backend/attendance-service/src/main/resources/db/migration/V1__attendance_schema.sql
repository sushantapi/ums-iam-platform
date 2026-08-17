CREATE TABLE hrms_attendance (
    id CHAR(36) PRIMARY KEY,
    organization_id CHAR(36) NOT NULL,
    employee_id CHAR(36) NOT NULL,
    work_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    check_in_at DATETIME NULL,
    check_out_at DATETIME NULL,
    notes VARCHAR(500) NULL,
    created_by CHAR(36) NOT NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX uk_attendance_org_employee_date
ON hrms_attendance(organization_id, employee_id, work_date);

CREATE INDEX idx_attendance_org_date
ON hrms_attendance(organization_id, work_date);

CREATE INDEX idx_attendance_org_employee
ON hrms_attendance(organization_id, employee_id);
