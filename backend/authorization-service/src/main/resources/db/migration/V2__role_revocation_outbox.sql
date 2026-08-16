CREATE TABLE role_revocation_outbox (
    event_id CHAR(36) PRIMARY KEY,
    assignment_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    revoked_by CHAR(36),
    revoked_at DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    published_at DATETIME(6),
    CONSTRAINT uk_role_revocation_outbox_assignment UNIQUE (assignment_id)
);

CREATE INDEX idx_role_revocation_outbox_status_created
    ON role_revocation_outbox(status, created_at);
