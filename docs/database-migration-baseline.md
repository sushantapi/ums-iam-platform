# Task 11 - Database Migration Baseline

Status: Implemented and validated against clean MySQL 8 databases on June 24, 2026.

## Stateful Service Checklist

- [x] authentication-service owns complete Flyway schema migrations
- [x] authentication-service seeds roles required by registration
- [x] user-service owns complete Flyway schema migrations
- [x] authorization-service zero-byte migration replaced with a complete schema
- [x] organization-service retains its existing schema migration
- [x] notification-service owns complete Flyway schema migrations
- [x] audit-service owns complete Flyway schema migrations
- [x] Docker, dev, UAT, and production profiles use Hibernate `validate`
- [x] Flyway is explicitly enabled with `baseline-on-migrate: false`
- [x] UUID columns use the repository `CHAR(36)` convention

## Migration Inventory

| Service | Migration | Tables |
|---|---|---|
| authentication-service | `V1__init_schema.sql` | `users`, `roles`, `user_roles`, `sessions`, `audit_logs` |
| authentication-service | `V2__seed_authentication_roles.sql` | Required authentication role seed data |
| user-service | `V1__init_schema.sql` | `user_profiles`, `user_preferences` |
| authorization-service | `V1__authorization_schema.sql` | `resources`, `roles`, `permissions`, `role_permissions`, `user_roles`, `policies` |
| organization-service | `V1__organization_schema.sql` | `organizations`, `organization_members` |
| notification-service | `V1__init_schema.sql` | `notification_templates`, `notification_events`, `notification_logs` |
| audit-service | `V1__init_schema.sql` | `audit_logs` |

## Clean Database Proof

Run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-clean-databases.ps1
```

The script:

1. Starts an isolated disposable MySQL 8 container.
2. Creates empty service databases only.
3. Boots each stateful service sequentially with Flyway enabled and Hibernate validation.
4. Fails if migration or schema validation prevents startup.
5. Deletes the temporary MySQL container unless `-KeepContainer` is supplied.

Logs are written under `.runlogs/migration-validation`.

This deliberately does not drop or mutate the developer's existing databases.

Existing non-empty environments that predate Flyway require an explicit, reviewed
adoption plan. Do not enable these baselines against an existing schema and assume
Flyway can infer its history; take a backup, reconcile the live schema, and baseline
at the verified version before deployment.

## Operational Rule

Do not use `ddl-auto: update` in any shared environment. Schema changes must be introduced as forward-only Flyway migrations and reviewed alongside the entity change.
