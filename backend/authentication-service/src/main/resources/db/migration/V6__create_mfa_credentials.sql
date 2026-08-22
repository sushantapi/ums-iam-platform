CREATE TABLE mfa_credentials (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    encrypted_secret VARCHAR(1024) NOT NULL,
    status VARCHAR(20) NOT NULL,
    setup_expires_at DATETIME(6),
    activated_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_auth_mfa_credentials_user UNIQUE (user_id),
    CONSTRAINT fk_auth_mfa_credentials_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_auth_mfa_credentials_status_expiry
    ON mfa_credentials(status, setup_expires_at);

CREATE TABLE mfa_recovery_codes (
    id CHAR(36) PRIMARY KEY,
    credential_id CHAR(36) NOT NULL,
    code_hash CHAR(64) NOT NULL,
    consumed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_auth_mfa_recovery_code_hash UNIQUE (credential_id, code_hash),
    CONSTRAINT fk_auth_mfa_recovery_codes_credential
        FOREIGN KEY (credential_id) REFERENCES mfa_credentials(id) ON DELETE CASCADE
);

CREATE INDEX idx_auth_mfa_recovery_codes_available
    ON mfa_recovery_codes(credential_id, consumed_at);
