ALTER TABLE hrms_salary_structures
    ADD COLUMN version_number INT NULL AFTER employee_id,
    ADD COLUMN supersedes_structure_id CHAR(36) NULL AFTER version_number,
    ADD COLUMN superseded_at DATETIME(6) NULL AFTER active,
    ADD COLUMN superseded_by CHAR(36) NULL AFTER superseded_at;

UPDATE hrms_salary_structures target
JOIN (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY organization_id, employee_id
            ORDER BY effective_from, id
        ) AS version_number,
        LAG(id) OVER (
            PARTITION BY organization_id, employee_id
            ORDER BY effective_from, id
        ) AS supersedes_structure_id
    FROM hrms_salary_structures
) ranked ON ranked.id = target.id
SET
    target.version_number = ranked.version_number,
    target.supersedes_structure_id = ranked.supersedes_structure_id;

ALTER TABLE hrms_salary_structures
    MODIFY COLUMN version_number INT NOT NULL;

ALTER TABLE hrms_salary_structures
    ADD CONSTRAINT chk_salary_structure_version_number
        CHECK (version_number > 0),
    ADD CONSTRAINT uk_salary_structure_org_employee_version
        UNIQUE (organization_id, employee_id, version_number),
    ADD CONSTRAINT uk_salary_structure_single_successor
        UNIQUE (supersedes_structure_id),
    ADD CONSTRAINT fk_salary_structure_supersedes
        FOREIGN KEY (supersedes_structure_id)
        REFERENCES hrms_salary_structures(id);

CREATE INDEX idx_salary_structure_org_employee_version
    ON hrms_salary_structures (organization_id, employee_id, version_number);
