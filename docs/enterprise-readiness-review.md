# UMS IAM Platform - Enterprise Readiness Review

Review date: 2026-06-12

## Executive Summary

The repository is a promising microservice prototype, not yet an enterprise-ready IAM
platform. It has sensible domain names, separate deployable services, asymmetric JWT
signing, refresh-token hashing, BCrypt password hashing, RabbitMQ events, and a shared
security module. However, the most important runtime controls are incomplete or
inconsistently wired.

The largest risks are:

1. RSA private keys are committed to Git and packaged with the application.
2. Authorization is not enforced consistently across services.
3. Organization APIs trust a caller-controlled identity header and do not check
   organization ownership or membership.
4. Authentication and authorization services maintain independent role stores.
5. Database migrations, constraints, API pagination, resilience, observability, CI,
   and meaningful automated tests are mostly absent.
6. Production configuration is incomplete and several service contracts reference
   endpoints or services that do not exist.

The current design should be hardened before adding payroll, attendance, leave, or
other HRMS modules. Adding more services now would multiply security and operational
inconsistencies.

## Scores

| Area | Score | Rationale |
|---|---:|---|
| Architecture | 4.5/10 | Good domain intent and service separation, but incomplete contracts, duplicated ownership, empty modules, no parent build, and inconsistent infrastructure |
| Security | 3.0/10 | Good cryptographic primitives, but committed private keys, public/internal endpoints, missing resource authorization, and incomplete token revocation |
| Scalability | 4.0/10 | Stateless access tokens and asynchronous events help, but there is no rate limiting, resilience, pagination, caching strategy, outbox, autoscaling, or production deployment platform |
| Maintainability | 4.5/10 | Familiar layered packages and shared modules help, but dependency drift, commented implementations, duplicate models/events, weak tests, and configuration sprawl increase change risk |

## 1. Project Structure

### Current shape

- `backend/` contains independently built Spring Boot services.
- `backend/common/common-security` contains JWT parsing and request authentication.
- `backend/common/common-events` contains shared RabbitMQ contracts.
- `backend/config-repo` contains environment-specific Spring Cloud Config files.
- `frontend/`, `infrastructure/`, and `scripts/` contain directory scaffolding but no
  implementation.
- `backend/audit-service` is empty even though README and admin clients depend on it.
- `backend/pom.xml` is empty, so there is no repository-level dependency management,
  reactor build, test aggregation, or quality gate.

### Assessment

The directory structure communicates the intended system clearly, but it overstates
the implemented platform. The README lists services and capabilities that are empty,
commented out, or only partially implemented. A production repository should make the
supported runtime topology unambiguous.

## 2. Service Boundaries

| Service | Intended ownership | Current assessment |
|---|---|---|
| API Gateway | Edge routing and token validation | JWT is parsed in a custom global filter, but the reactive security chain is not configured as an OAuth2 resource server and the filter does not populate a reactive security context |
| Authentication | Credentials, login, tokens, sessions | Strongest implemented service; owns users and a role relation, which overlaps authorization-service |
| Authorization | Roles, permissions, assignments | Domain is appropriate, but JWT authentication is not consistently wired and role assignment lacks adequate method-level protection |
| User | User profile and preferences | Reasonable profile boundary; internal endpoints are public and admin contract does not match implemented endpoints |
| Organization | Organization and membership | Correct domain candidate, but currently has no active Spring Security dependency/configuration and no tenant authorization checks |
| Notification | Templates, delivery, logs, retries | Useful boundary; notification logs and test endpoints are public, retry execution is unfinished |
| Admin | Backend-for-frontend/orchestration | References `session-service`, `role-service`, and `audit-service`, which are not deployed services in this repository |
| Config | Centralized configuration | Native filesystem mode is acceptable for local development, not for secure production configuration |
| Discovery | Eureka registry | Suitable for the current Spring Cloud approach; must not be publicly exposed |
| Audit | Expected audit ownership | Empty and therefore unavailable |

### Boundary recommendations

- Make authentication the sole owner of credentials and sessions.
- Make authorization the sole owner of roles, permissions, and grants.
- Stop embedding authoritative roles in long-lived access tokens, or introduce a
  versioned entitlement snapshot with explicit invalidation.
- Treat admin-service as a BFF only after its downstream contracts are real and tested.
- Use events for profile provisioning, but use an outbox and idempotent consumers.
- Define an explicit tenant context contract used by every tenant-owned service.

## 3. Package Structure

Most business services follow familiar packages such as `controller`, `service`,
`repository`, `entity`, `dto`, `config`, and `exception`. This is understandable for
the current size.

Problems:

- The organization repository package is misspelled `repositoty`.
- Large commented-out implementations remain in production source.
- Event classes are duplicated between `common-events` and notification-service.
- AuthorizationServiceApplication scans `com.ums.auth`, crossing service package
  boundaries.
- Shared security is auto-configured through broad component scanning rather than
  narrow, conditional beans.
- Maven versions vary across services: Spring Boot 3.5.0/3.5.14, Spring Cloud
  2025.0.0/2025.0.2, JJWT 0.11.5/0.12.5/0.12.7, and differing Springdoc versions.

Recommendation: introduce a real parent BOM and build, remove unused dependencies,
standardize package conventions, and keep shared libraries limited to stable technical
contracts rather than domain ownership.

## 4. Security Implementation

### Positive controls

- BCrypt strength 12 is used for passwords.
- Registration and login DTOs have basic validation.
- Login errors avoid obvious user enumeration.
- Failed login counting and temporary account lockout are implemented.
- Access and refresh tokens use RS256.
- Refresh tokens are stored as SHA-256 hashes.
- User-role and organization-member duplicate checks exist at application level, with
  some database uniqueness support.

### Critical findings

1. **Committed signing keys**

   Private keys exist in:

   - `backend/authentication-service/src/main/resources/keys/private_key.pem`
   - `backend/config-repo/keys/private_key.pem`

   These keys must be considered compromised, rotated, removed from Git history, and
   replaced with KMS/Vault/Secrets Manager-backed signing.

2. **Organization service lacks active authentication**

   Its security configuration is commented out and its POM does not include the shared
   security module or Spring Security. Organization resources can therefore be reached
   without the same controls used by other services.

3. **Caller-controlled identity**

   Organization creation reads `X-User-Id`, while the gateway writes
   `X-Authenticated-User`. Even if the names matched, downstream services must strip
   external identity headers at the edge and derive identity from a verified principal,
   not trust arbitrary request headers.

4. **Missing resource authorization**

   Organization get/member APIs do not verify owner, organization admin, or membership.
   Any authenticated or direct-network caller can enumerate organizations and members
   or add users.

5. **Public internal and operational endpoints**

   User-service permits `/api/v1/internal/**`. Notification-service permits test
   endpoints, all actuator endpoints, and all notification log endpoints. Authentication
   and authorization services permit all actuator endpoints.

6. **Role assignment is under-protected**

   `/api/v1/authorization/assign-role` has no `@PreAuthorize` restriction. Method
   security exists elsewhere but is not applied consistently.

7. **Direct service exposure**

   Docker Compose publishes every service, RabbitMQ management, Eureka, and Config
   Server to the host. Production should expose only the ingress/gateway and tightly
   controlled operations endpoints.

8. **No service-to-service authentication**

   Internal calls rely on bearer forwarding, public internal endpoints, or direct
   network trust. There is no mTLS, workload identity, signed internal token, or
   authorization policy.

## 5. JWT Implementation

### What works

- RS256 signing and public-key verification are appropriate.
- Access token lifetime defaults to 15 minutes.
- Refresh token lifetime defaults to 7 days.
- Tokens include `jti`, `iss`, `iat`, `exp`, and a token type.
- Refresh rotation updates the stored token hash.

### Gaps

- No `aud` claim or audience validation.
- No `kid`, JWKS endpoint, or overlapping-key rotation strategy.
- Services load static classpath keys.
- Resource servers do not consistently validate token type.
- Logout blacklists access tokens only in authentication-service; gateway and downstream
  validators never consult the blacklist.
- Logout deliberately fails open when Redis is unavailable.
- Logout does not revoke the refresh-token session.
- Refresh checks the JWT and session hash but does not explicitly compare the token
  subject to the session user.
- Refresh does not explicitly check `session.expiresAt`.
- User suspension, deletion, password changes, or role changes do not revoke existing
  access tokens.
- Authentication and authorization roles can drift, while access tokens carry roles
  from authentication-service.
- Token parsing errors are swallowed in the shared servlet filter, reducing diagnostics
  and producing inconsistent error responses.

### Target design

Use Spring Authorization Server or a mature external IdP unless custom token issuance
is a hard requirement. Publish JWKS, use `kid`, validate issuer and audience, rotate
refresh tokens with reuse detection, revoke session families, and propagate entitlement
version changes. Prefer short-lived access tokens and centralized session revocation
over a distributed per-request blacklist.

## 6. Database Design

### Positive elements

- Database-per-service is reflected in configuration.
- UUID identifiers are common across services.
- Organization schema has a Flyway migration, foreign key, unique slug, and unique
  organization/user membership.
- Refresh tokens are not stored in plaintext.

### Gaps

- Only one migration file exists across all business services.
- Development and Docker profiles use `ddl-auto: update`.
- Several production service configuration files are empty.
- Entity nullability and length constraints do not consistently match SQL constraints.
- `sessions.refresh_token_hash` lacks an explicit unique index.
- Session lookup and cleanup fields lack documented indexes.
- User provider/external ID lacks a composite uniqueness constraint.
- User preferences lack uniqueness on `user_id`.
- Authorization permissions allow uniqueness by name only, not necessarily by
  `(resource, action)`.
- Organization membership has no user foreign key because user data is remote; this
  requires explicit lifecycle/event consistency handling.
- There is no tenant ID in IAM grants or user profiles, so SaaS multi-tenancy cannot be
  enforced consistently.
- There is no soft-delete/audit/version strategy across entities.
- No backup, restore, retention, archival, partitioning, or data residency design is
  present.

## 7. API Design

### Positive elements

- Most public APIs use `/api/v1`.
- Controllers generally use DTOs rather than exposing entities.
- Some request validation and global exception handling exist.
- OpenAPI dependencies are present in several services.

### Gaps

- Response envelopes and error formats differ by service.
- No RFC 9457 Problem Details standard is used.
- Collection APIs generally return unbounded `List` results.
- No consistent pagination, sorting, filtering, or field selection.
- No idempotency keys for create, invite, role assignment, payroll, or notification
  operations.
- No optimistic concurrency (`ETag`, version fields, or `If-Match`).
- Internal API naming is inconsistent (`/internal`, `/api/v1/internal`).
- Gateway forwarding headers do not match organization-service expectations.
- Discovery locator can expose every registered service route unless explicitly
  constrained.
- There are no consumer-driven contract tests.
- Swagger and actuator exposure is not environment-restricted.

## 8. Production Readiness Gaps

### Delivery and quality

- No GitHub Actions workflow is implemented.
- There are only nine basic context-load tests for roughly 241 main Java files.
- Authentication tests fail without external config because test configuration is not
  isolated.
- Eight services compile with tests skipped, which confirms source-level buildability
  but not runtime correctness.
- No unit, integration, security, contract, migration, or end-to-end test suites.
- No SAST, dependency scanning, secret scanning, SBOM, image scanning, or signed images.

### Reliability

- No timeouts, retries, circuit breakers, bulkheads, or fallback policy for Feign calls.
- Organization-service hardcodes a localhost URL and bypasses discovery.
- No transactional outbox; database commit and event publish can diverge.
- Consumer idempotency is partial and not standardized.
- Retry/DLQ behavior is incomplete and some implementation is commented out.
- No readiness/startup probes or graceful shutdown policy.

### Observability and operations

- No central metrics, traces, dashboards, alerts, or SLOs.
- No consistent correlation ID propagation.
- Audit service is absent.
- Logs include `System.out` statements and no documented PII redaction policy.
- No runbooks, incident response process, backup restore test, or disaster recovery plan.

### Platform and product

- Frontend directories are empty.
- Kubernetes, Helm, Terraform, Nginx, and deployment script directories are empty.
- Docker Compose omits MySQL, Redis, admin-service, and audit-service.
- Images use mutable broad tags and containers have no health checks or resource limits.
- MFA, email verification, password reset, SCIM, SSO/OIDC federation, WebAuthn, API keys,
  consent, and delegated administration are described but not fully implemented.

## HRMS Guide Alignment

`HRMS_Full_Guide_v2.docx` is a useful target-state guide, but it is not a description of
the current repository.

Key differences:

- The guide targets 11 domain services; the repository implements a smaller IAM core
  and has an empty audit-service.
- The guide uses PostgreSQL and Kafka; the repository uses MySQL and RabbitMQ.
- The guide's authentication port and some API paths differ from current code.
- Employee, payroll, attendance, leave, tax, reimbursement, onboarding, document,
  reporting, and frontend modules are not implemented.
- Kubernetes manifests, HPA, ingress, secrets, and CI/CD described in the guide are not
  present.

The enterprise roadmap should first stabilize IAM as a secure platform foundation, then
add HRMS domains one bounded context at a time.

## Recommended Target Architecture

1. An ingress/WAF exposes only API Gateway and the IdP authorization endpoints.
2. Authentication uses a standards-compliant OAuth 2.1/OIDC authorization server.
3. Every API is an OAuth2 resource server validating issuer, audience, signature, and
   scopes from JWKS.
4. Authorization owns tenant-scoped roles, permissions, policies, and grant versions.
5. Services derive tenant and subject from verified claims and enforce resource-level
   authorization.
6. Internal calls use workload identity/mTLS and explicit service scopes.
7. Each service owns its schema and Flyway migrations.
8. State changes publish through a transactional outbox to RabbitMQ.
9. OpenTelemetry provides logs, metrics, traces, and correlation.
10. Kubernetes deploys immutable signed images with probes, limits, autoscaling, network
    policies, and secrets from an external secrets manager.

