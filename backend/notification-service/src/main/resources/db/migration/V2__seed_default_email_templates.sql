INSERT INTO notification_templates (template_code, subject, body, channel, active, created_at, updated_at)
SELECT 'WELCOME_EMAIL', 'Welcome to UMS', 'Hello {{name}}, welcome to UMS.', 'EMAIL', TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE template_code = 'WELCOME_EMAIL');

INSERT INTO notification_templates (template_code, subject, body, channel, active, created_at, updated_at)
SELECT 'EMAIL_VERIFICATION', 'Verify your UMS account', 'Hello {{name}}, your verification code is {{verificationLink}}.', 'EMAIL', TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE template_code = 'EMAIL_VERIFICATION');

INSERT INTO notification_templates (template_code, subject, body, channel, active, created_at, updated_at)
SELECT 'PASSWORD_RESET', 'Reset your UMS password', 'Hello {{name}}, your password reset code is {{resetLink}}.', 'EMAIL', TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE template_code = 'PASSWORD_RESET');

INSERT INTO notification_templates (template_code, subject, body, channel, active, created_at, updated_at)
SELECT 'MFA_OTP', 'Your UMS security code', 'Your UMS security code is {{otp}}.', 'EMAIL', TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE template_code = 'MFA_OTP');

INSERT INTO notification_templates (template_code, subject, body, channel, active, created_at, updated_at)
SELECT 'ORGANIZATION_INVITATION', 'You have been invited to an organization', 'You have been invited to {{organizationName}}. Use this invitation: {{inviteLink}}', 'EMAIL', TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE template_code = 'ORGANIZATION_INVITATION');

INSERT INTO notification_templates (template_code, subject, body, channel, active, created_at, updated_at)
SELECT 'ORGANIZATION_CREATED', 'Your organization is ready', 'Your organization {{organizationName}} has been created successfully.', 'EMAIL', TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE template_code = 'ORGANIZATION_CREATED');
