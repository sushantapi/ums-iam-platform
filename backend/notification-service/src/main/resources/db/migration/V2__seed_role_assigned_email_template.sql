INSERT INTO notification_templates (template_code, subject, body, channel, active, created_at, updated_at)
SELECT 'ROLE_ASSIGNED',
       'UMS role assigned',
       'Hello {{name}}, the role {{roleName}} has been assigned to your UMS account.',
       'EMAIL',
       TRUE,
       NOW(6),
       NOW(6)
WHERE NOT EXISTS (
    SELECT 1
    FROM notification_templates
    WHERE template_code = 'ROLE_ASSIGNED'
);
