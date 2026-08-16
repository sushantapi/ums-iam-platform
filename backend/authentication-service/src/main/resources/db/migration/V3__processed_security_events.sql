CREATE TABLE processed_security_events (
    event_id CHAR(36) PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    user_id CHAR(36) NOT NULL,
    processed_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_processed_security_events_user_processed
    ON processed_security_events(user_id, processed_at);
