ALTER TABLE hrms_salary_structures
    ADD COLUMN version_number INT NOT NULL DEFAULT 1;

ALTER TABLE hrms_salary_structures
    ADD COLUMN supersedes_structure_id CHAR(36) NULL;

ALTER TABLE hrms_salary_structures
    ADD COLUMN superseded_at DATETIME(6) NULL;

ALTER TABLE hrms_salary_structures
    ADD COLUMN superseded_by CHAR(36) NULL;

CREATE TEMPORARY TABLE hrms_salary_structure_version_backfill AS
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
FROM hrms_salary_structures;

UPDATE hrms_salary_structures
SET
    version_number = (
        SELECT backfill.version_number
        FROM hrms_salary_structure_version_backfill backfill
        WHERE backfill.id = hrms_salary_structures.id
    ),
    supersedes_structure_id = (
        SELECT backfill.supersedes_structure_id
        FROM hrms_salary_structure_version_backfill backfill
        WHERE backfill.id = hrms_salary_structures.id
    );

DROP TABLE hrms_salary_structure_version_backfill;

ALTER TABLE hrms_salary_structures
    ALTER COLUMN version_number DROP DEFAULT;

ALTER TABLE hrms_salary_structures
    ADD CONSTRAINT chk_salary_structure_version_number
        CHECK (version_number > 0);

ALTER TABLE hrms_salary_structures
    ADD CONSTRAINT uk_salary_structure_org_employee_version
        UNIQUE (organization_id, employee_id, version_number);

ALTER TABLE hrms_salary_structures
    ADD CONSTRAINT uk_salary_structure_single_successor
        UNIQUE (supersedes_structure_id);

ALTER TABLE hrms_salary_structures
    ADD CONSTRAINT fk_salary_structure_supersedes
        FOREIGN KEY (supersedes_structure_id)
        REFERENCES hrms_salary_structures(id);
