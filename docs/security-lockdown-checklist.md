# Task 10 - Platform Lockdown Sweep

Status: Phase 0 sweep complete

## 1. API Gateway

- [x] Strips client-supplied identity, role, permission, and gateway-secret headers
- [x] Injects trusted identity headers only after JWT authentication
- [x] Rejects refresh tokens on protected routes
- [x] Exposes only register, login, and refresh anonymously
- [x] Does not route internal service namespaces
- [x] Uses explicit routes with discovery auto-routing disabled
- [x] Uses public verification key material only
- [x] Has tests for header stripping, identity injection, and token type

## 2. Authentication Service

- [x] Only register, login, refresh, health, and info are public
- [x] Logout requires trusted gateway identity
- [x] Admin session APIs require trusted gateway identity and explicit authority
- [x] Internal namespace requires the internal service secret
- [x] Direct-JWT trust filter is absent
- [x] Swagger is not public
- [x] Refresh validates issuer, type, JTI, session, user status, expiry, and token hash
- [x] Refresh rotation is concurrency protected
- [x] Logout and administrative revocation are audited
- [x] Authentication-service is the only JWT signer

## 3. Authorization Service

- [x] Replaced legacy direct-JWT authentication with trusted gateway identity
- [x] Internal routes use only the internal service secret
- [x] Internal and external filters have separate route families
- [x] Internal role assignment is separate and protected
- [x] Role and permission mutation requires explicit write authority
- [x] User-role reads require explicit read authority
- [x] Test controller is dev-profile only
- [x] Swagger is disabled
- [x] Only health and info are public

## 4. User Service

- [x] External routes reject direct JWTs and spoofed identity
- [x] Trusted gateway identity is accepted
- [x] Internal routes reject gateway-secret-only and direct-JWT requests
- [x] Internal routes accept the internal service secret
- [x] Self-service identity comes from the authenticated principal
- [x] Administrative user lookup has explicit authority
- [x] Internal listing is bounded and paginated
- [x] Removed obsolete JWT verification dependencies and key configuration

## 5. Organization Service

- [x] External routes use trusted gateway identity
- [x] User lookup Feign call uses the internal service secret
- [x] No organization mutation route is public
- [x] Boundary tests cover direct JWT, spoofing, trusted gateway, health, and Swagger
- [x] Only health and info are public

Organization-service currently provides no internal controller route, so internal-provider tests are not applicable.

## 6. Notification Service

- [x] External routes use trusted gateway identity
- [x] Internal namespace uses the internal service secret
- [x] Test controllers are dev-profile only and not anonymous
- [x] Templates and logs use explicit authorities
- [x] Email addresses are masked in operational logs
- [x] Removed obsolete JWT key configuration
- [x] Only health and info are public

## 7. Audit Service

- [x] External audit reads require gateway trust and audit authority
- [x] Support has no blanket audit access
- [x] Internal audit query requires the internal service secret
- [x] Query bounds and filter validation are enforced
- [x] Wildcard escaping is active
- [x] Ingestion remains broker-only and sanitized
- [x] Only health and info are public

## 8. Admin Service

- [x] External routes use trusted gateway identity
- [x] Internal namespace uses the internal service secret
- [x] No direct JWT stack or bearer forwarding remains
- [x] Feign calls use canonical internal routes and the internal service secret
- [x] User, audit, dashboard, and role domains have separate authorities
- [x] Privileged role escalation by `AUTH_ADMIN` is blocked
- [x] User and audit reads are bounded and paginated
- [x] Unsupported filters fail closed
- [x] Only health and info are public

## 9. Gateway Route Matrix

| Gateway predicate | Target | Downstream path |
|---|---|---|
| `/api/v1/auth/**` | authentication-service | unchanged |
| `/api/v1/admin/sessions/**` | authentication-service | unchanged |
| `/api/v1/admin/users/*/sessions/**` | authentication-service | unchanged |
| `/api/v1/users/**` | user-service | unchanged |
| `/api/v1/organizations/**` | organization-service | unchanged |
| `/api/v1/authorization/**` | authorization-service | unchanged |
| `/api/v1/roles/**` | authorization-service | unchanged |
| `/api/v1/permissions/**` | authorization-service | unchanged |
| `/api/v1/notifications/**` | notification-service | unchanged |
| `/api/v1/templates/**` | notification-service | unchanged |
| `/api/v1/admin/**` | admin-service | unchanged |
| `/api/v1/audit/events/**` | audit-service | unchanged |

Authentication session routes are ordered before the broad admin-service route. Docker `StripPrefix` filters and servlet context paths were removed so controllers receive their declared `/api/v1/**` paths.

## 10. Configuration and Secrets

- [x] Gateway-trusted services define `INTERNAL_GATEWAY_SECRET` in dev, Docker, UAT, and production
- [x] Internal providers and callers define `INTERNAL_SERVICE_SECRET`
- [x] Only authentication-service has JWT private-key configuration
- [x] User, authorization, organization, notification, audit, and admin services do not validate direct JWTs
- [x] No tracked private PEM/key file remains
- [x] No symmetric `jwt.secret` property remains
- [x] UAT and production internal secrets have no committed defaults
- [x] Docker actuator exposure is health/info only
- [x] Config-server repository path is environment-driven
- [x] Docker Compose configuration parses successfully

## 11. Verification Results

| Module | Command | Result |
|---|---|---|
| api-gateway | `mvn clean test` | 4 passed |
| authentication-service | `mvn test` | 19 passed |
| user-service | `mvn test` | 8 passed |
| authorization-service | `mvn clean test` | 6 passed |
| organization-service | `mvn test` | 4 passed |
| notification-service | `mvn test` | 13 passed |
| audit-service | `mvn test` | 16 passed |
| admin-service | `mvn test` | 10 passed |
| config-service | `mvn -DskipTests compile` | passed |
| discovery-service | `mvn -DskipTests compile` | passed |
| repository | `docker compose config --quiet` | passed |
| repository | `git diff --check` | passed; line-ending warnings only |

## Phase 0 Lockdown Defects Fixed

- Authorization-service trusted direct bearer JWTs instead of gateway identity.
- Authorization-service had overlapping legacy internal paths.
- Gateway public allow-list included unimplemented password/email routes.
- Gateway explicit route sets were incomplete and inconsistent by profile.
- Gateway discovery locator could expose unintended service-ID routes.
- Docker gateway routes stripped `/api/v1` even though controllers declared the full prefix.
- Multiple Docker service context paths double-prefixed controller routes.
- Several Docker profiles exposed metrics and Prometheus endpoints.
- JWT public-key configuration remained in services that no longer validate JWTs.
- Notification UAT/production profiles lacked explicit internal secrets.
- Config-service used a workstation-specific absolute repository path.

## Deferred Production Backlog

- Enforce access-token revocation/blacklist checks at the gateway.
- Add and validate JWT audience claims.
- Replace the platform-wide shared internal secret with per-service identity, signed service tokens, or mTLS.
- Keep config-service, discovery-service, databases, Redis, RabbitMQ management, and direct service ports on private networks; the local Compose file publishes ports for developer convenience.
- Add authentication to the config/discovery control plane if network isolation cannot be guaranteed.
- Add pagination and hard response limits to notification log/template list APIs.
- Add organization-scoped admin context and enforcement where `ORG_ADMIN` reads platform role/permission data.
- Continue the existing audit backlog: publisher provenance, idempotency, append-only/tamper evidence, and deeper redaction.
- Continue notification reliability backlog: deduplication, distributed retry coordination, and DLQ operations.
- Standardize Spring Boot/Spring Cloud dependency versions across modules.
