CREATE TABLE organizations (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    owner_id CHAR(36) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL
);

CREATE TABLE organization_members (
    id CHAR(36) PRIMARY KEY,
    organization_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    role VARCHAR(30) NOT NULL,
    joined_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_org_member_org
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_org_user
ON organization_members(organization_id, user_id);