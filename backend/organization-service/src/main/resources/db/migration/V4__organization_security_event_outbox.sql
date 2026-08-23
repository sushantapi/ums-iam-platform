CREATE TABLE organization_security_event_outbox (
    event_id CHAR(36) PRIMARY KEY,
    organization_id CHAR(36) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    updated_by CHAR(36) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NULL,
    published_at TIMESTAMP NULL,
    last_error_type VARCHAR(255) NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_org_security_event_outbox_org
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_org_security_event_outbox_dispatch
    ON organization_security_event_outbox(status, next_attempt_at, created_at);
