CREATE TABLE organization_logo_assets (
    id CHAR(36) PRIMARY KEY,
    organization_id CHAR(36) NOT NULL,
    asset_version INT NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    byte_size BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT fk_org_logo_asset_org
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_org_logo_asset_version
ON organization_logo_assets(organization_id, asset_version);

CREATE INDEX idx_org_logo_asset_org
ON organization_logo_assets(organization_id);

ALTER TABLE organization_profiles
    ADD CONSTRAINT fk_org_profile_logo_asset
    FOREIGN KEY (logo_asset_id)
    REFERENCES organization_logo_assets(id);
