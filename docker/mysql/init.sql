-- Create all service databases
CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS authorization_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS organization_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS audit_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS admin_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create users with proper privileges
CREATE USER IF NOT EXISTS 'ums_user'@'%' IDENTIFIED BY 'ums_secure_password_change_me';

-- Grant privileges to all databases
GRANT ALL PRIVILEGES ON auth_db.* TO 'ums_user'@'%';
GRANT ALL PRIVILEGES ON user_db.* TO 'ums_user'@'%';
GRANT ALL PRIVILEGES ON authorization_db.* TO 'ums_user'@'%';
GRANT ALL PRIVILEGES ON organization_db.* TO 'ums_user'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'ums_user'@'%';
GRANT ALL PRIVILEGES ON audit_db.* TO 'ums_user'@'%';
GRANT ALL PRIVILEGES ON admin_db.* TO 'ums_user'@'%';

FLUSH PRIVILEGES;
