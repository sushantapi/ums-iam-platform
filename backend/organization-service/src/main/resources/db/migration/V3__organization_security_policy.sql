CREATE TABLE organization_security_policies (
    organization_id CHAR(36) PRIMARY KEY,
    require_mfa BOOLEAN NOT NULL DEFAULT FALSE,
    updated_by CHAR(36) NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT fk_org_security_policy_org
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE
);
