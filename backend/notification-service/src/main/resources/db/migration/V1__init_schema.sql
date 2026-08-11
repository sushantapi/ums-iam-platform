CREATE TABLE notification_templates (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    template_code VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    body LONGTEXT,
    channel VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT uk_notification_templates_code UNIQUE (template_code)
);

CREATE TABLE notification_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    recipient VARCHAR(255),
    recipient_email VARCHAR(255),
    template_code VARCHAR(255),
    correlation_id VARCHAR(255),
    channel VARCHAR(255),
    notification_type VARCHAR(255),
    status VARCHAR(255),
    payload LONGTEXT,
    error_message LONGTEXT,
    retry_count INT DEFAULT 0,
    created_at DATETIME(6),
    processed_at DATETIME(6)
);

CREATE INDEX idx_notification_events_status_retry
    ON notification_events(status, retry_count);
CREATE INDEX idx_notification_events_correlation
    ON notification_events(correlation_id);
CREATE INDEX idx_notification_events_created
    ON notification_events(created_at);

CREATE TABLE notification_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(255),
    recipient VARCHAR(255),
    recipient_email VARCHAR(255),
    subject VARCHAR(255),
    status VARCHAR(255),
    error_message TEXT,
    sent_at DATETIME(6),
    created_at DATETIME(6)
);

CREATE INDEX idx_notification_logs_status_created
    ON notification_logs(status, created_at);
CREATE INDEX idx_notification_logs_recipient_created
    ON notification_logs(recipient_email, created_at);
CREATE INDEX idx_notification_logs_event_created
    ON notification_logs(event_type, created_at);
