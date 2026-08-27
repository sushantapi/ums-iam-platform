ALTER TABLE role_revocation_outbox
    ADD COLUMN scope_type VARCHAR(32) NULL AFTER role_name,
    ADD COLUMN scope_id VARCHAR(128) NULL AFTER scope_type;
