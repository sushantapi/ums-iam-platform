CREATE TABLE organization_invitations (
    id CHAR(36) PRIMARY KEY,
    organization_id CHAR(36) NOT NULL,
    normalized_email VARCHAR(320) NOT NULL,
    active_email_key VARCHAR(320),
    role VARCHAR(30) NOT NULL,
    inviter_id CHAR(36) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    last_sent_at TIMESTAMP(6),
    accepted_at TIMESTAMP(6),
    revoked_at TIMESTAMP(6),
    expired_at TIMESTAMP(6),
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),

    CONSTRAINT fk_org_invitation_org
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_org_invitation_token_hash
        UNIQUE (token_hash),

    CONSTRAINT uk_org_invitation_active_email
        UNIQUE (organization_id, active_email_key),

    CONSTRAINT chk_org_invitation_role
        CHECK (role <> 'OWNER'),

    CONSTRAINT chk_org_invitation_active_key
        CHECK (
            (status = 'PENDING' AND active_email_key IS NOT NULL)
            OR
            (status <> 'PENDING' AND active_email_key IS NULL)
        )
);

CREATE INDEX idx_org_invitation_org_status
ON organization_invitations(organization_id, status);

CREATE INDEX idx_org_invitation_email
ON organization_invitations(normalized_email);
