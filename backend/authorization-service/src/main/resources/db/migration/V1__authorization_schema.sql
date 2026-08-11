CREATE TABLE resources (
    id CHAR(36) PRIMARY KEY,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_authorization_resources_code UNIQUE (code)
);

CREATE TABLE roles (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_authorization_roles_name UNIQUE (name)
);

CREATE TABLE permissions (
    id CHAR(36) PRIMARY KEY,
    code VARCHAR(120) NOT NULL,
    resource_id CHAR(36) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_authorization_permissions_code UNIQUE (code),
    CONSTRAINT uk_permission_resource_action UNIQUE (resource_id, action),
    CONSTRAINT fk_authorization_permissions_resource
        FOREIGN KEY (resource_id) REFERENCES resources(id)
);

CREATE TABLE role_permissions (
    id CHAR(36) PRIMARY KEY,
    role_id CHAR(36) NOT NULL,
    permission_id CHAR(36) NOT NULL,
    assigned_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_authorization_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_authorization_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_authorization_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE TABLE user_roles (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,
    assigned_by CHAR(36),
    scope_type VARCHAR(30) NOT NULL DEFAULT 'PLATFORM',
    scope_id VARCHAR(36) NOT NULL DEFAULT '*',
    expires_at DATETIME(6),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_user_role_scope UNIQUE (user_id, role_id, scope_type, scope_id),
    CONSTRAINT fk_authorization_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE INDEX idx_authorization_user_roles_user_active
    ON user_roles(user_id, active, expires_at);
CREATE INDEX idx_authorization_user_roles_scope
    ON user_roles(scope_type, scope_id, active);

CREATE TABLE policies (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    role_id CHAR(36) NOT NULL,
    permission_id CHAR(36) NOT NULL,
    effect VARCHAR(10) NOT NULL,
    condition_json JSON,
    priority INT NOT NULL DEFAULT 100,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_policy_name UNIQUE (name),
    CONSTRAINT fk_authorization_policies_role
        FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_authorization_policies_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

CREATE INDEX idx_authorization_policies_evaluation
    ON policies(role_id, permission_id, active, priority);
