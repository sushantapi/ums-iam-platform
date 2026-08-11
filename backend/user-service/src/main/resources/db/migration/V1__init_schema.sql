CREATE TABLE user_profiles (
    user_id CHAR(36) PRIMARY KEY,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    email VARCHAR(255),
    mobile VARCHAR(255),
    avatar_url VARCHAR(255),
    address VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    country VARCHAR(255),
    zip_code VARCHAR(255),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT uk_user_profiles_email UNIQUE (email)
);

CREATE INDEX idx_user_profiles_name ON user_profiles(last_name, first_name);

CREATE TABLE user_preferences (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    language VARCHAR(255),
    theme VARCHAR(255),
    email_notification BOOLEAN,
    sms_notification BOOLEAN,
    CONSTRAINT uk_user_preferences_user UNIQUE (user_id)
);

CREATE INDEX idx_user_preferences_user ON user_preferences(user_id);
