# Task 12 - Environment and Profile Matrix

Status: implemented and validated on June 25, 2026. The matrix is validated by
`scripts/validate-profile-matrix.ps1`.

## Configuration ownership

- Service-local `application.yaml` files define the application name, selected
  profile, optional Config Server import, and safe cross-profile defaults.
- `backend/config-repo/application-{profile}.yml` owns shared Eureka, RabbitMQ,
  and Redis endpoints.
- `backend/config-repo/{service}-{profile}.yml` owns the service port and
  environment-specific runtime configuration.
- Discovery does not import Config Server. This avoids a discovery/config
  bootstrap cycle.
- Config Server always runs with the `native` profile and reads the mounted
  config repository.

## Service matrix

All listed ports are internal application ports and remain identical across
dev, Docker, UAT, and production.

| Service | Port | Database | RabbitMQ | Redis | JWT/key responsibility | Required trust secret |
|---|---:|---|---|---|---|---|
| api-gateway | 8080 | — | — | — | RSA public verification key | `INTERNAL_GATEWAY_SECRET` |
| authentication-service | 8086 | `AUTH_DB_NAME` | publish | sessions/revocation | RSA private signing key and public key | gateway + service |
| user-service | 8081 | `USER_DB_NAME` | publish | — | none | gateway + service |
| authorization-service | 8082 | `AUTHORIZATION_DB_NAME` | — | authorization cache wiring | none | gateway + service |
| organization-service | 8087 | `ORGANIZATION_DB_NAME` | publish | — | none | gateway + service |
| notification-service | 8085 | `NOTIFICATION_DB_NAME` | consume | — | none | gateway + service |
| audit-service | 8089 | `AUDIT_DB_NAME` | consume | — | none | gateway + service |
| admin-service | 8088 | — | — | — | none | gateway + service |
| config-service | 8888 | — | — | — | none | none |
| discovery-service | 8761 | — | — | — | none | none |

## Profile policy

| Profile | Infrastructure addressing | Secrets | Database SSL | Error details |
|---|---|---|---|---|
| dev | localhost defaults | explicit local-only defaults | local policy | development-friendly |
| docker | Compose DNS names and mounted key files | environment variables | disabled inside local Docker network | enabled |
| uat | environment variables only | required environment variables | `MYSQL_USE_SSL`, default true | disabled |
| prod | environment variables only | required environment variables | `MYSQL_USE_SSL`, default true | disabled |

Every stateful profile enables Flyway, disables automatic baselining, and uses
Hibernate `ddl-auto: validate`. UAT and production profiles require database,
messaging, mail, key, Eureka, and internal-secret values from the deployment
environment; no environment-specific hostnames or credentials are committed.

Only authentication-service receives the JWT private key. API Gateway receives
the public verification key. Gateway-trusted business services do not carry
unused direct-JWT verification configuration.

## Validation

Run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/validate-profile-matrix.ps1
```

The validation checks:

1. All 32 application/profile files exist.
2. Ports are stable and non-conflicting.
3. Stateful profiles include datasource, Flyway, and Hibernate validation.
4. Runtime dependencies such as RabbitMQ, Redis, mail, Eureka, and keys are
   present where required.
5. UAT and production require injected trust secrets.
6. Config Server boots in native mode and resolves every service/profile pair.

Task 11's `validate-clean-databases.ps1` remains the authoritative clean-schema
boot proof. Run both scripts before merging configuration or schema changes.
