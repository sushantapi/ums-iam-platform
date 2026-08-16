CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(255),
    service_name VARCHAR(255),
    user_id VARCHAR(255),
    user_email VARCHAR(255),
    action VARCHAR(255),
    entity_type VARCHAR(255),
    entity_id VARCHAR(255),
    details VARCHAR(5000),
    ip_address VARCHAR(255),
    created_at DATETIME(6)
);

CREATE INDEX idx_audit_created ON audit_logs(created_at);
CREATE INDEX idx_audit_event_created ON audit_logs(event_type, created_at);
CREATE INDEX idx_audit_service_created ON audit_logs(service_name, created_at);
CREATE INDEX idx_audit_user_created ON audit_logs(user_id, created_at);
CREATE INDEX idx_audit_entity_created ON audit_logs(entity_type, entity_id, created_at);
