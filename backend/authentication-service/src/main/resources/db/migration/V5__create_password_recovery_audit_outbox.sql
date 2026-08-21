CREATE TABLE password_recovery_audit_outbox (
    id CHAR(36) PRIMARY KEY,
    event_type VARCHAR(150) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    user_id VARCHAR(64),
    user_email VARCHAR(150),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(100),
    details VARCHAR(1000),
    ip_address VARCHAR(255),
    event_timestamp DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    published_at DATETIME(6),
    last_error VARCHAR(255)
);

CREATE INDEX idx_auth_password_recovery_audit_outbox_pending
    ON password_recovery_audit_outbox(status, next_attempt_at, created_at);
