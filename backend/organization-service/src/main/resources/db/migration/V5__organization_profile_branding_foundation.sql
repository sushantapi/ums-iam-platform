CREATE TABLE organization_profiles (
    organization_id CHAR(36) PRIMARY KEY,
    legal_name VARCHAR(255),
    display_name VARCHAR(255),
    registered_address VARCHAR(1000),
    business_email VARCHAR(255),
    business_phone VARCHAR(50),
    website VARCHAR(255),
    default_currency CHAR(3),
    payroll_country CHAR(2),
    payslip_footer_text VARCHAR(500),
    authorized_signatory_label VARCHAR(255),
    logo_asset_id CHAR(36),
    logo_asset_version INT,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT fk_org_profile_org
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
        ON DELETE CASCADE
);
