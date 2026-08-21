CREATE TABLE password_reset_tokens (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6),
    revoked_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_auth_password_reset_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_auth_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_auth_password_reset_tokens_user_state
    ON password_reset_tokens(user_id, consumed_at, revoked_at, expires_at);

CREATE INDEX idx_auth_password_reset_tokens_expiry
    ON password_reset_tokens(expires_at);
