# UMS IAM Production Review Pack

Review date: 2026-06-23

## 1. Current Architecture

UMS IAM is a Spring Boot 3 / Java 21 microservice platform with a React admin portal. The intended runtime topology is:

- API Gateway on port 8080 as the edge entry point.
- Discovery Service / Eureka on port 8761.
- Config Service on port 8888 backed by `backend/config-repo`.
- Authentication Service on port 8086 for registration, login, refresh tokens, logout, sessions, and credential-owned users.
- Authorization Service on port 8082 for roles, permissions, user-role assignments, and user authorization snapshots.
- User Service on port 8081 for user profile and preference data.
- Organization Service on port 8087 for organizations and organization membership.
- Notification Service on port 8085 for templates, email delivery, notification logs, retries, and event consumers.
- Audit Service on port 8089 for consuming audit events and exposing paginated audit event queries.
- Admin Service on port 8088 as a backend-for-frontend for admin dashboard, users, roles, sessions, and audit views.
- Frontend Admin Portal under `frontend/admin-portal`, using Vite, React 19, React Router, Zustand, and mockable API calls.
- MySQL, RabbitMQ, and Redis in Docker Compose.
- Optional monitoring compose stack and Prometheus/Logstash configs under `observability`.

High-level data flow:

```text
Admin Portal / API Clients
        |
        v
API Gateway
        |
        +--> Authentication Service --> MySQL auth_db, Redis, RabbitMQ audit/user events
        +--> Authorization Service  --> MySQL authorization_db
        +--> User Service           --> MySQL user_db
        +--> Organization Service   --> MySQL organization_db, RabbitMQ org events
        +--> Notification Service   --> MySQL notification_db, RabbitMQ consumers, SMTP
        +--> Audit Service          --> MySQL audit_db, RabbitMQ audit consumer
        +--> Admin Service          --> Feign calls to downstream services

Discovery Service and Config Service support service discovery and centralized config.
```

Current codebase indicators:

- Backend main Java files: 293.
- Backend test files: 18.
- Flyway migrations found: 2, only for authorization and organization.
- GitHub workflow files: none currently present.
- Root `backend/pom.xml` is empty, so there is no effective Maven reactor build or shared dependency management from the root backend project.

## 2. Current Feature Status

| Area | Status | Notes |
|---|---|---|
| API Gateway routing | Partial | Routes/config exist, and a JWT global filter exists, but the active WebFlux security chain permits every request. |
| Gateway JWT validation | Partial | `GatewayJwtAuthenticationFilter` validates tokens and forwards identity headers, but gateway security still has `anyExchange().permitAll()`. There is also an unused HS256-style `JwtService` alongside RSA validator code. |
| Registration | Partial | Creates auth user, assigns local EMPLOYEE role, calls authorization default-role endpoint, emits user/audit events, creates refresh session. No outbox, no email verification enforcement. |
| Login | Partial | BCrypt password check, failed-attempt lockout, session creation, authorization lookup for roles/permissions, audit event publish. |
| Refresh token | Partial | Refresh token hash lookup and rotation exist. Missing explicit session expiry check, subject/session-user consistency check, reuse detection, and session-family revocation. |
| Logout | Partial | Access-token JTI is blacklisted in auth Redis. Gateway and downstream validators do not appear to consult this blacklist, and refresh sessions are not revoked by logout. |
| Admin sessions | Partial | Auth service exposes admin session list/revoke endpoints protected by method security. |
| Authorization roles/permissions | Partial | Role, permission, assignment, and check endpoints exist. Some role endpoints use `@PreAuthorize`; sensitive assignment endpoints are not consistently protected. |
| User profile | Partial | User profile/preference endpoints exist. Internal user endpoint is permitted without authentication. |
| Organization | Partial | Create/get/add member/list members exist. Security configuration is effectively absent/commented, and resource-level membership/owner authorization is missing. |
| Notification | Partial | Template, notification log, email strategy, event consumers, and retry scheduler exist. Test endpoints and notification log endpoints are public. Some provider methods remain TODO or placeholder. |
| Audit | Partial | Audit event consumer and paginated query controller exist. No security config was found for audit-service, so access policy must be confirmed/enforced. |
| Admin BFF | Early partial | Dashboard/users/roles/audit controllers exist, but several admin portal routes expect endpoints not implemented by admin-service. Some controllers only return simple lists and have commented lifecycle operations. |
| Admin portal | Partial | Real React app with dashboard, users, organizations, roles, permissions, grants, audit, and sessions screens. It can run against mocks. Backend contract coverage is incomplete. |
| Config | Partial | Config repo contains dev/docker/prod/uat files, but production files are thin/incomplete and key handling is unsafe. |
| Docker Compose | Local-dev ready, not production ready | Compose includes major services and dependencies with health checks, but publishes internal services and databases to host ports and uses local images/tags. |
| Observability | Early partial | Prometheus/logstash files and actuator/prometheus exposure exist, but no verified dashboards, alerts, tracing propagation, SLOs, or runbooks. |
| CI/CD | Missing | `.github/workflows` contains no workflow files. |

## 3. All Existing Flows

### Authentication Flows

1. Register
   - `POST /api/v1/auth/register`
   - Creates a local user in authentication-service.
   - Assigns local `EMPLOYEE` role.
   - Calls authorization-service internal default-role endpoint.
   - Emits `UserRegisteredEvent`.
   - Publishes audit event.
   - Creates refresh-token session.
   - Returns access and refresh tokens.

2. Login
   - `POST /api/v1/auth/login`
   - Normalizes email.
   - Checks account lock/suspended/deleted states.
   - Verifies password with BCrypt.
   - Resets failed attempts.
   - Creates session with refresh token hash.
   - Fetches roles/permissions from authorization-service.
   - Returns JWT access token plus refresh token.

3. Refresh
   - `POST /api/v1/auth/refresh`
   - Validates refresh JWT.
   - Finds session by SHA-256 refresh token hash.
   - Rejects revoked sessions.
   - Rotates refresh token hash.
   - Issues new access/refresh token pair.

4. Logout
   - `POST /api/v1/auth/logout`
   - Extracts bearer token.
   - Adds access-token JTI to Redis blacklist for remaining token lifetime.
   - Publishes audit event.
   - Does not revoke refresh-token session.

5. Admin session operations
   - `GET /api/v1/admin/sessions`
   - `GET /api/v1/admin/users/{userId}/sessions`
   - `POST /api/v1/admin/sessions/{sessionId}/revoke`
   - `POST /api/v1/admin/users/{userId}/sessions/revoke-all`

### Authorization Flows

1. Assign role
   - `POST /api/v1/authorization/assign-role`
   - Assigns a role to a user.
   - Currently lacks method-level authorization.

2. Assign permission
   - `POST /api/v1/authorization/assign-permission`
   - Assigns permission to a role.
   - Currently lacks method-level authorization.

3. Get user permissions
   - `GET /api/v1/authorization/users/{id}/permissions`

4. Permission check
   - `GET /api/v1/authorization/check?userId=...&permission=...`

5. Internal authorization snapshot
   - `GET /api/v1/internal/users/{userId}/authorization`
   - Used by authentication-service during token generation.

6. Assign default role
   - `POST /api/v1/internal/users/{userId}/roles/default`
   - Used by authentication-service after registration.

7. Role catalog
   - `POST /api/v1/roles`
   - `GET /api/v1/roles`

8. Permission catalog
   - `POST /api/v1/permissions`
   - `GET /api/v1/permissions`
   - `GET /api/v1/permissions/{id}`
   - `DELETE /api/v1/permissions/{id}`

### User Flows

1. Current user profile
   - `GET /api/v1/users/me`

2. Update profile
   - `PUT /api/v1/users/profile`

3. Get user by ID
   - `GET /api/v1/users/{userId}`

4. Delete profile
   - `DELETE /api/v1/users/profile`

5. Internal user lookup
   - `GET /api/v1/internal/users/{userId}`
   - Internal route is currently permit-all.

### Organization Flows

1. Create organization
   - `POST /api/v1/organizations`
   - Reads `X-Authenticated-User` when present, otherwise falls back to request `userId`.
   - Creates owner membership.
   - Publishes organization event.

2. Get organization
   - `GET /api/v1/organizations/{organizationId}`

3. Add member
   - `POST /api/v1/organizations/{organizationId}/members`

4. List members
   - `GET /api/v1/organizations/{organizationId}/members`

### Notification Flows

1. Template management
   - `POST /api/v1/templates`
   - `GET /api/v1/templates`
   - `GET /api/v1/templates/{templateCode}`

2. Notification logs
   - `GET /api/v1/notifications`
   - `GET /api/v1/notifications/status/{status}`
   - `GET /api/v1/notifications/recipient/{email}`
   - Currently permit-all.

3. Test endpoints
   - `GET /api/test/welcome`
   - `POST /test/publish`
   - Currently permit-all.

4. Event consumers
   - User, auth, role, and organization events trigger notification handling.

5. Retry scheduler
   - Retry component exists, but production-grade DLQ/retry behavior needs verification.

### Audit Flows

1. Audit event ingestion
   - Rabbit listener consumes `AuditEvent` from the shared audit queue and stores `AuditLog`.

2. Audit query
   - `GET /api/v1/audit/events`
   - Supports page, size, actor, target, organizationId, eventType, serviceName, outcome, from, and to.

3. Audit detail
   - `GET /api/v1/audit/events/{eventId}`

### Admin BFF / Portal Flows

1. Dashboard
   - Backend: `GET /api/v1/admin/dashboard`
   - Frontend route: `/dashboard`

2. Admin users
   - Backend currently: `GET /api/v1/admin/users`
   - Frontend expects additional detail, roles, organizations, sessions, activate, suspend, unlock, reset password endpoints.

3. Admin roles
   - Backend currently: `POST /api/v1/admin/roles/assign`
   - Frontend expects role list/detail/permissions/assignments/grants endpoints.

4. Admin audit
   - Backend currently: `GET /api/v1/admin/audit/logs`
   - Frontend also calls direct audit endpoints under `/api/v1/audit/events`.

5. Sessions
   - Frontend calls auth/admin session endpoints.

6. Organizations
   - Frontend expects admin organization list/detail/members/invitations/security-policy endpoints.
   - Matching backend admin endpoints are not currently complete.

## 4. Security Issues

### Critical

1. Gateway permits all traffic
   - Active gateway security chain uses `anyExchange().permitAll()`.
   - The gateway JWT filter may still run as a global filter, but the security chain does not enforce route authentication or authorization.
   - Fix before any production or shared environment.

2. Private keys are committed and packaged
   - Private RSA keys exist under authentication-service resources and config-repo.
   - Treat these keys as compromised.
   - Rotate keys, remove from current tree and Git history, move signing to Vault/KMS/Secrets Manager, and publish public keys through JWKS.

3. Organization service is not protected
   - Security config is commented/effectively absent.
   - Organization APIs trust `X-Authenticated-User` if present and otherwise accept request body/user ID input.
   - Get/member APIs do not enforce owner/admin/member checks.

4. Public internal endpoints
   - User-service permits `/api/v1/internal/**`.
   - Authorization-service permits `/api/v1/internal/**`.
   - Notification-service permits notification logs and test endpoints.
   - Actuator endpoints are broadly permit-all in multiple services.

5. Role/permission mutation endpoints are under-protected
   - Authorization assignment endpoints lack consistent `@PreAuthorize`.
   - Admin role/user controllers use `hasRole('ADMIN')`, while other controllers use `SUPER_ADMIN`/`ORG_ADMIN`; role vocabulary is inconsistent.

### High

6. JWT design is incomplete for distributed production
   - No `aud` claim validation.
   - No `kid` or JWKS rotation path.
   - Token revocation blacklist is local to authentication-service.
   - Downstream services do not consistently validate token type.
   - Access token includes roles/permissions snapshots with no entitlement version or invalidation.

7. Service-to-service trust is weak
   - Internal calls depend on forwarded bearer tokens, public internal endpoints, or network placement.
   - No mTLS, workload identity, service-scoped tokens, or internal authorization policy.

8. Admin portal stores access token in persisted Zustand state
   - This puts bearer tokens into browser storage.
   - Use secure cookie/session strategy or a carefully reviewed token storage model with refresh rotation and XSS hardening.

9. Debug logging and `System.out` in security-sensitive paths
   - Gateway logs user IDs and roles.
   - Several controllers/security configs print debug messages.
   - Replace with structured, redacted logs.

### Medium

10. Docker Compose exposes internal services and dependencies
    - MySQL, Redis, RabbitMQ, Eureka, Config Server, and all microservices are published to host ports.
    - For production, expose only ingress/gateway and tightly controlled operations surfaces.

11. Config uses unsafe defaults/placeholders
    - `.env` and config files include placeholder secrets and default passwords.
    - MySQL URLs use `allowPublicKeyRetrieval=true`.
    - Dev configs use `ddl-auto: update`.

12. API security model is inconsistent
    - Some controllers use method security, some rely on URL auth, some are public, some have no security config.
    - Error responses and exception behavior differ by service.

## 5. Production Readiness Gaps

### Build And Delivery

- No root Maven reactor build.
- No effective shared dependency/BOM strategy across services.
- No CI workflow.
- No automated quality gates for formatting, unit tests, integration tests, SAST, dependency scanning, secret scanning, container scanning, SBOM, or image signing.
- Docker images use local names like `authentication-service:1.0`; no registry, immutable digest, or promotion model.

### Testing

- Only 18 backend test files for 293 backend Java files.
- Test coverage is mostly application/context-level plus a few service tests.
- Missing contract tests between admin portal, admin-service, auth-service, authorization-service, user-service, audit-service, and organization-service.
- Missing security tests for route protection and authorization decisions.
- Missing migration tests.
- Missing end-to-end login/admin/user/role/org/audit flows.

### Data And Migrations

- Only authorization and organization have Flyway migrations.
- Several services still rely on Hibernate `ddl-auto` in dev/config paths.
- Need explicit indexes/constraints for sessions, refresh token hashes, user uniqueness, preferences, role/permission grants, audit query dimensions, notification logs, and tenant-scoped access.
- Need backup, restore, retention, deletion, archival, and data residency policies.

### Reliability

- No transactional outbox for events.
- Rabbit publish and database commit can diverge.
- Consumer idempotency is not standardized.
- No consistent retries, DLQs, backoff, or poison-message handling policy.
- Feign clients need timeouts, retries/circuit breakers, and failure behavior.
- No documented graceful shutdown or startup/readiness strategy beyond basic Docker health checks.

### Observability

- Actuator/prometheus exposure exists in config, but the full observability path is not production-proven.
- Need correlation ID propagation through gateway, Feign, RabbitMQ, logs, metrics, and traces.
- Need dashboards and alerts for auth failures, lockouts, token refresh anomalies, queue lag, mail failures, DB saturation, gateway 4xx/5xx, p95/p99 latency, and service health.
- Need audit completeness checks and tamper-resistance strategy.

### API And Product

- Admin portal expects many endpoints that are not implemented yet.
- Many collection endpoints return unbounded lists or lack consistent page response contracts.
- No standard problem-details error format.
- No consistent idempotency support for create/assign/invite/send operations.
- No optimistic concurrency/version checks for mutable resources.
- IAM product gaps remain: email verification enforcement, MFA enrollment/verification, password reset, SSO/OIDC federation, SCIM, service accounts/API keys, access reviews, break-glass, signing-key management, and tenant policy administration.

### Platform

- No Kubernetes/Helm/Terraform production deployment path in active code.
- No external secrets integration.
- No network policies.
- No resource requests/limits.
- No autoscaling policy.
- No disaster recovery/runbook package.

## 6. Exact Build Order

### Phase 0: Stop-The-Line Security Fixes

1. Replace gateway `permitAll` with explicit public routes plus authenticated protected routes.
2. Strip inbound identity headers at the gateway before adding trusted `X-Authenticated-User` headers.
3. Protect all internal endpoints; allow only gateway/service identity, not public callers.
4. Remove private keys from source/resources/config repo, rotate keys, and introduce secure secret-backed key loading.
5. Add `@PreAuthorize` or equivalent policy checks to authorization role/permission mutation endpoints.
6. Add organization-service security and enforce owner/admin/member checks on every organization resource.
7. Lock down notification test/log endpoints and actuator exposure.

### Phase 1: Make The Repo Buildable And Testable

1. Create a real backend parent Maven project with modules for every service and common library.
2. Standardize Spring Boot, Spring Cloud, JJWT, Springdoc, Lombok, Flyway, and testing dependency versions.
3. Add CI with compile, unit tests, frontend type-check/build, secret scan, dependency scan, and Docker build validation.
4. Remove stale commented code and `System.out` debug prints.
5. Add service-level test profiles that do not require manually running external infrastructure.

### Phase 2: Stabilize Authentication And Authorization

1. Add JWT `aud`, `kid`, JWKS, and key rotation.
2. Make all resource servers validate issuer, audience, signature, token type, expiry, and required authorities consistently.
3. Decide ownership: authentication owns credentials/sessions; authorization owns roles/permissions/grants.
4. Stop duplicating authoritative roles in authentication-service, or implement a clear synchronization/invalidation model.
5. Fix refresh flow: session expiry check, token subject/session user check, reuse detection, refresh family revocation, logout refresh-session revocation.
6. Add entitlement versioning or short-lived authorization cache invalidation.

### Phase 3: Align Admin Portal And Backend Contracts

1. Freeze API contracts for admin dashboard, users, sessions, roles, permissions, grants, organizations, invitations, security policy, and audit.
2. Implement missing admin-service endpoints required by `frontend/admin-portal/src/lib/api.ts`.
3. Add pagination/search/filter contracts for every admin list.
4. Add contract tests for the admin portal API surface.
5. Decide which APIs go through admin-service BFF versus direct domain-service routes, then make the frontend consistent.

### Phase 4: Data Migrations And Domain Hardening

1. Add Flyway migrations for authentication, user, notification, audit, and admin-owned schemas.
2. Add required unique constraints and indexes.
3. Replace `ddl-auto: update` with migration-only schema management outside throwaway local dev.
4. Add tenant/organization context where needed for IAM grants and user-visible admin data.
5. Add optimistic locking/version fields to mutable core entities.
6. Add data retention and deletion policies for sessions, audit, notification logs, and user PII.

### Phase 5: Event Reliability

1. Introduce transactional outbox for auth, organization, authorization, notification, and audit-producing events.
2. Add idempotency keys and consumer dedupe tables where events mutate state or send external messages.
3. Define exchange/queue/DLQ/retry conventions centrally.
4. Add poison-message handling and operational replay tools.
5. Add integration tests for event publishing and consuming.

### Phase 6: Observability And Operations

1. Add correlation ID middleware at gateway and propagate through Feign/RabbitMQ.
2. Add OpenTelemetry traces for HTTP, Feign, database, and RabbitMQ.
3. Build dashboards for gateway, auth, authorization, audit, queues, DB, and notification delivery.
4. Add alerts and SLOs.
5. Write runbooks for login outage, token/key incident, queue backlog, DB failure, mail failure, and compromised key rotation.

### Phase 7: Production Deployment

1. Move from Docker Compose to Kubernetes or the chosen production platform.
2. Add external secrets, network policies, resource limits, health/readiness/startup probes, and autoscaling.
3. Publish immutable signed images with SBOMs.
4. Expose only ingress/gateway and controlled ops endpoints.
5. Add backup/restore automation and DR tests.
6. Run load, soak, security, and failover tests before launch.

### Phase 8: Product Completion

1. Email verification enforcement.
2. Password reset end-to-end.
3. MFA enrollment, challenge, recovery codes, and admin reset.
4. OIDC/SSO federation and client management.
5. SCIM provisioning.
6. Service accounts and API keys.
7. Access requests, access reviews, break-glass, and policy governance.
8. Organization security policy UI and backend.

## Production Verdict

This repo is a strong IAM prototype and local integration platform, but it is not production ready today. The main reason is not missing polish; it is that the current edge security, internal endpoint exposure, key management, organization authorization, incomplete admin contracts, and missing CI/test/migration gates create real production risk.

The correct next move is to harden the IAM foundation before adding more HRMS/product modules. Fix gateway/authz/org security first, then make the repo build and test reliably, then align frontend/backend contracts.
