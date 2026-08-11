CREATE TABLE users (
    id CHAR(36) PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    external_id VARCHAR(100),
    provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    last_login_at DATETIME(6),
    CONSTRAINT uk_auth_users_email UNIQUE (email)
);

CREATE TABLE roles (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT uk_auth_roles_name UNIQUE (name)
);

CREATE TABLE user_roles (
    user_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_auth_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_auth_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE sessions (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    refresh_token_hash VARCHAR(500) NOT NULL,
    ip_address VARCHAR(255),
    device_info VARCHAR(255),
    client VARCHAR(255),
    organization_id CHAR(36),
    expires_at DATETIME(6),
    last_seen_at DATETIME(6),
    revoked_at DATETIME(6),
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6),
    CONSTRAINT uk_auth_sessions_refresh_hash UNIQUE (refresh_token_hash),
    CONSTRAINT fk_auth_sessions_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_auth_sessions_user ON sessions(user_id);
CREATE INDEX idx_auth_sessions_user_status ON sessions(user_id, revoked, expires_at);
CREATE INDEX idx_auth_sessions_expiry ON sessions(expires_at);
CREATE INDEX idx_auth_sessions_organization ON sessions(organization_id);

CREATE TABLE audit_logs (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36),
    event_type VARCHAR(255),
    ip_address VARCHAR(255),
    status VARCHAR(255),
    created_at DATETIME(6)
);

CREATE INDEX idx_auth_audit_user ON audit_logs(user_id);
CREATE INDEX idx_auth_audit_event_created ON audit_logs(event_type, created_at);
