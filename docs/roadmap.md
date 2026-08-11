# UMS IAM Platform Enterprise Roadmap

This roadmap prioritizes a secure, operable IAM foundation before expanding into the
HRMS domains described in `HRMS_Full_Guide_v2.docx`.

## Phase 0 - Emergency Security Remediation (Week 0-1)

### Exit criteria

- No private key or production credential is stored in Git or application images.
- Organization, internal, notification, actuator, config, discovery, and broker
  endpoints cannot be reached anonymously.
- Only the gateway/ingress is externally exposed.
- Organization-service remains private behind the gateway while it uses trusted
  gateway headers; browser/mobile clients and public networks must not reach it
  directly.

### Work

- Rotate the committed RSA key pair immediately.
- Remove private keys from current files and Git history.
- Store signing keys in AWS KMS, HashiCorp Vault, or an equivalent HSM-backed service.
- Add secret scanning and block commits containing private keys or credentials.
- Disable or protect all test controllers, Swagger, Eureka, Config Server, RabbitMQ
  management, and actuator endpoints outside development.
- Remove public access to `/api/v1/internal/**` and notification logs.
- Add Spring Security and shared resource-server configuration to organization-service.
- Strip incoming identity headers at the gateway and stop trusting caller-provided
  `X-User-Id`.
- Require administrator authorization for role and permission mutations.
- Add organization owner/admin/member checks to every organization resource operation.
- Require an internal gateway trust marker, such as a shared internal gateway secret
  or mTLS identity, before downstream services accept gateway-propagated identity
  headers.

## Phase 1 - Build and Dependency Baseline (Week 1-2)

### Exit criteria

- One command builds and tests the complete repository.
- All services use one approved Spring Boot, Spring Cloud, JJWT, and Springdoc version.
- CI blocks merge on compilation, tests, formatting, vulnerability, and secret checks.

### Work

- Create a real root Maven parent and reactor build.
- Centralize dependency and plugin management.
- Remove duplicate and unused JWT libraries from services using common-security.
- Add Maven Enforcer, compiler warnings, Spotless, Checkstyle, SpotBugs, and JaCoCo.
- Create GitHub Actions for build, test, SAST, dependency review, secret scanning, SBOM,
  container scanning, and artifact publishing.
- Add isolated `test` profiles with Testcontainers for MySQL, Redis, and RabbitMQ.
- Replace context-only tests with unit, slice, integration, and security tests.
- Establish minimum coverage for security and domain-critical code.

## Phase 2 - Identity and Token Platform (Week 2-5)

### Exit criteria

- OAuth 2.1/OIDC flows are standards compliant.
- Keys rotate without downtime.
- Refresh token theft and reuse are detected.
- Account or entitlement changes can invalidate sessions predictably.

### Work

- Adopt Spring Authorization Server or a managed IdP.
- Publish a JWKS endpoint with `kid` and overlapping active keys.
- Add issuer, audience, token type, scope, and clock-skew validation.
- Implement refresh-token families, one-time rotation, reuse detection, and session
  revocation.
- Revoke refresh sessions on logout, password change, suspension, deletion, and security
  events.
- Add password reset and verified-email workflows with single-use, short-lived tokens.
- Implement MFA using TOTP and WebAuthn; add recovery codes and step-up authentication.
- Add secure cookie/BFF guidance for browser clients instead of storing refresh tokens in
  browser storage.
- Add rate limits and bot/brute-force protection for login, registration, refresh, reset,
  and verification endpoints.

## Phase 3 - Authorization and Multi-Tenancy (Week 4-7)

### Exit criteria

- Authorization-service is the only authority for grants and permissions.
- Every tenant-owned record and API is protected by tenant and resource policy.
- Role changes take effect within a defined maximum delay.

### Work

- Remove the duplicate authoritative role model from authentication-service.
- Define tenant-scoped RBAC tables for roles, permissions, grants, groups, and delegated
  administration.
- Add `tenant_id` to tenant-owned data and indexes.
- Define platform roles separately from organization roles.
- Add permission checks at controller/service boundaries with deny-by-default policies.
- Introduce entitlement versioning or short-lived cached authorization decisions.
- Add policy tests for cross-tenant access, IDOR, privilege escalation, and confused
  deputy scenarios.
- Add service identity and service scopes for internal APIs.

## Phase 4 - Data Integrity and Event Reliability (Week 6-9)

### Exit criteria

- Every schema change is versioned and repeatable.
- A committed business transaction cannot silently lose its integration event.
- Consumers safely handle duplicate and out-of-order delivery.

### Work

- Add Flyway migrations to authentication, authorization, user, notification, admin, and
  future services.
- Set production Hibernate schema mode to `validate`; remove `ddl-auto: update`.
- Add missing unique, foreign-key, lookup, cleanup, and tenant composite indexes.
- Add entity version columns for optimistic concurrency.
- Implement transactional outbox and publisher relay.
- Standardize event envelopes with event ID, schema version, tenant, subject, timestamp,
  correlation ID, and causation ID.
- Add inbox/idempotency records for consumers.
- Configure retry policies, parking queues/DLQs, replay tooling, and poison-message
  alerts.
- Define retention, archival, encryption, backup, restore, and deletion policies.

## Phase 5 - API and Service Reliability (Week 8-11)

### Exit criteria

- APIs have stable contracts, bounded queries, consistent errors, and resilience rules.
- Failure of one dependency does not cascade through the platform.

### Work

- Standardize API paths, resource names, status codes, and RFC 9457 Problem Details.
- Add cursor or page-based pagination to all collection endpoints.
- Add filtering, sorting, validation limits, and maximum page sizes.
- Add idempotency keys to non-idempotent business operations.
- Add `ETag`/`If-Match` or version-based concurrency for mutable resources.
- Replace hardcoded Feign URLs with service discovery or environment-managed endpoints.
- Add connect/read timeouts, Resilience4j circuit breakers, retries with jitter, and
  bulkheads.
- Add consumer-driven contract tests and generated OpenAPI validation.
- Disable automatic gateway discovery routing; define an explicit route allowlist.
- Version internal APIs separately and authenticate every internal call.

## Phase 6 - Observability, Audit, and Compliance (Week 10-13)

### Exit criteria

- Operators can trace a request across services and detect SLO violations.
- Security-sensitive actions produce immutable, searchable audit evidence.

### Work

- Implement audit-service with append-only storage and tamper-evident retention.
- Audit authentication, token, role, permission, tenant, admin, export, and sensitive-data
  operations.
- Add OpenTelemetry instrumentation and W3C trace-context propagation.
- Export Prometheus metrics and structured JSON logs.
- Add dashboards and alerts for latency, error rate, saturation, login abuse, token
  failures, queue lag, DLQs, email failure, and database health.
- Define SLIs/SLOs and error budgets for authentication and authorization.
- Add PII classification, log redaction, encryption, retention, access review, and data
  subject request procedures.
- Document incident response, key compromise, account takeover, and disaster recovery
  runbooks.

## Phase 7 - Production Platform (Week 12-16)

### Exit criteria

- The platform can be deployed, upgraded, scaled, and rolled back safely.
- Recovery objectives are tested rather than assumed.

### Work

- Complete Docker Compose for local MySQL, Redis, RabbitMQ, all services, and health
  checks.
- Use pinned image digests, non-root users, read-only filesystems, minimal base images,
  and dropped Linux capabilities.
- Create Kubernetes/Helm manifests with startup, readiness, and liveness probes.
- Add requests/limits, PodDisruptionBudgets, HPA, topology spread, and anti-affinity.
- Add NetworkPolicies and external secret injection.
- Add ingress TLS, WAF, request-size limits, and rate limiting.
- Provision infrastructure through Terraform with separate dev/UAT/prod accounts.
- Implement rolling/canary deployment, database migration gates, and automated rollback.
- Test backup restore, regional recovery, and documented RPO/RTO targets.

## Phase 8 - Enterprise IAM Capabilities (Week 15-20)

### Exit criteria

- The platform supports enterprise federation and automated identity lifecycle.

### Work

- Add OIDC and SAML federation.
- Add SCIM 2.0 provisioning and deprovisioning.
- Add group-to-role mapping and just-in-time provisioning.
- Add API clients/service accounts with scoped credentials and rotation.
- Add access review, certification, separation-of-duties rules, and privileged access
  workflows.
- Add organization invitations, domain verification, delegated administration, and
  tenant policy configuration.
- Add session/device management and user-visible security history.

## Phase 9 - HRMS Domain Expansion (Week 18+)

Start only after Phases 0-6 meet their exit criteria.

### Recommended order

1. Employee and department management
2. Document vault and onboarding/offboarding
3. Attendance and shifts
4. Leave and approval workflows
5. Payroll and salary structures
6. Tax, statutory compliance, reimbursement, and reporting

### Rules for each new domain

- Define bounded context, owner, APIs, events, data classification, and tenant policy
  before implementation.
- Add migrations, threat model, unit/integration/contract/security tests, dashboards,
  alerts, and runbooks before production release.
- Keep MySQL/RabbitMQ unless an architecture decision record justifies migration to the
  PostgreSQL/Kafka stack proposed by the HRMS guide.
- Do not share databases across services.
- Do not copy identity, role, or authorization logic into HRMS services.

## Release Gates

An enterprise production release should require all of the following:

- No critical/high exploitable security findings.
- No secrets in source history or images.
- Passing unit, integration, security, migration, contract, and smoke tests.
- Tested key rotation and session revocation.
- Tested backup restore and disaster recovery.
- Tenant-isolation and privilege-escalation test suites pass.
- SLO dashboards and paging alerts are active.
- Runbooks and on-call ownership are assigned.
- Data retention, audit, privacy, and access-review controls are approved.
