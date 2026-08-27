INSERT INTO notification_templates (template_code, subject, body, channel, active, created_at, updated_at)
SELECT 'ROLE_ASSIGNED', 'Your UMS IAM access has been updated', 'Hello {{name}},\n\nThe {{roleName}} role has been assigned to your account.\n\nRole: {{roleName}}\nScope: {{scopeType}}\nScope ID: {{scopeId}}\n\nIf you did not expect this change, please contact your administrator.\n\nUMS IAM Security Team', 'EMAIL', TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE template_code = 'ROLE_ASSIGNED');

INSERT INTO notification_templates (template_code, subject, body, channel, active, created_at, updated_at)
SELECT 'ROLE_REVOKED', 'Your UMS IAM access has changed', 'Hello {{name}},\n\nThe {{roleName}} role has been removed from your account.\n\nRole: {{roleName}}\nScope: {{scopeType}}\nScope ID: {{scopeId}}\n\nIf you believe this was unexpected, please contact your administrator.\n\nUMS IAM Security Team', 'EMAIL', TRUE, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE template_code = 'ROLE_REVOKED');
