# UMS IAM + HRMS Platform

**IAM-first HRMS for growing teams**, built with Java 21, Spring Boot microservices, React, MySQL, Redis, RabbitMQ and Docker.

UMS combines centralized identity and access management with practical HR operations so an employee can use one organizational identity across authentication, authorization, employee records, attendance, leave, payroll and payslips.

## Product journey

```text
Identity
  ↓
Organization
  ↓
Role + Permissions
  ↓
Employee
  ↓
Attendance / Leave
  ↓
Payroll
  ↓
Payslip
  ↓
Sessions + Audit
```

The platform is designed around a shared IAM identity and tenant boundary. HRMS services do not create a second login system; they reuse UMS identity, organization and authorization contracts.

## Current capabilities

| Area | Current capability |
| --- | --- |
| Authentication | Registration, login, JWT access tokens, refresh-token rotation, logout, password reset and email verification flows |
| Security | MFA flow, gateway JWT validation, trusted identity headers, token/session revocation and fail-closed authorization boundaries |
| Authorization | Roles, permissions, grants, privileged-role protection and capability-based Admin Portal access |
| User management | User list/detail, profile/status lifecycle, activate, suspend and unlock operations |
| Organization management | Organizations, members, administration and tenant-aware access checks |
| Session security | Active session visibility, session revocation and revoked-token rejection |
| Audit | Security/admin activity persisted and exposed through the Admin Portal |
| Notifications | Event-driven welcome, verification, password-reset and organization-related email flows |
| Admin Portal | Dashboard, Users, Organizations, Roles, Permissions, Grants, Sessions, Audit and Security surfaces |
| Employee HRMS | Employee list/detail plus organization-linked employee records |
| HR master data | Departments and designations |
| Attendance | Attendance management/read flows |
| Leave | Leave requests and approval-oriented HRMS flow |
| Payroll | Salary structures, payroll runs, payroll entries and finalized payroll workflow |
| Salary versioning | Salary-structure supersede/versioning instead of overwriting historical compensation data |
| Statutory payroll | PF, ESI, TDS and tax-regime fields captured in payroll structures/entries |
| Payslips | Persisted payroll-entry based payslip preview/download flow |
| Company settings | HRMS company/payroll settings surface |
| Deployment | Production Compose baseline, Caddy HTTPS reverse proxy, GHCR image publishing, backup/restore scripts and rollback guidance |

> The root README is intentionally limited to capabilities present in the repository. Future enterprise features are listed separately below and are not presented as completed functionality.

## Admin Portal routes

The React Admin Portal currently includes protected routes for:

- Dashboard
- Users
- Organizations
- Roles
- Permissions
- Grants
- Sessions
- Audit
- Security / MFA
- HRMS Company Settings
- Employees
- Departments
- Designations
- Attendance
- Leave
- Payroll

See `frontend/admin-portal/src/app/router.tsx` and `frontend/admin-portal/src/features/hrms/routes.tsx` for the current route definitions.

## Backend modules

The backend Maven reactor currently contains two shared modules and fourteen runtime services:

### Shared modules

- `common-events`
- `common-security`

### Platform services

- Config Service
- Discovery Service (Eureka)
- Authentication Service
- User Service
- Authorization Service
- Organization Service
- Audit Service
- Notification Service
- Admin Service / BFF
- API Gateway

### HRMS services

- Employee Service
- Attendance Service
- Leave Service
- Payroll Service

The canonical module list is maintained in `backend/pom.xml`.

## Architecture

```text
                         CLIENTS
                ┌──────────┴──────────┐
                │                     │
          Admin Portal          Future clients
                │                     │
                └──────────┬──────────┘
                           ▼
                     API GATEWAY
                           │
          ┌────────────────┼────────────────┐
          │                │                │
 Authentication      Authorization    Organization
          │                │                │
          └──────┬─────────┴───────┬────────┘
                 │                 │
              Admin BFF          HRMS
                 │                 │
                 │        ┌────────┼────────┐
                 │        ▼        ▼        ▼
                 │    Employee  Attendance Leave
                 │        │
                 │      Payroll
                 │
          Audit / Notification

             RabbitMQ + Redis
          Database per service
```

### Architecture rules

- **Database per service** — no cross-service SQL joins or foreign keys across service-owned databases.
- **IAM owns authentication and authorization** — HRMS does not maintain passwords or a separate JWT system.
- **REST/Feign for immediate validation/read paths**.
- **RabbitMQ events for state propagation, audit and notifications**.
- **Transactional outbox + idempotent consumers** are used where durable cross-service delivery matters.
- **Flyway** owns schema migrations for service databases.
- The **Admin Service acts as a BFF/facade** for administrative workflows where a stable frontend contract is preferable to exposing internal service structure.

More architecture material is available under [`docs/architecture`](docs/architecture).

## Technology stack

### Backend

- Java 21
- Spring Boot 3
- Spring Security
- Spring Cloud
- Spring Cloud Gateway
- Eureka
- OpenFeign
- Maven
- Flyway

### Data and messaging

- MySQL
- Redis
- RabbitMQ

### Frontend

- React
- TypeScript
- Vite
- React Router

### Delivery

- Docker / Docker Compose
- Caddy for production HTTPS
- GitHub Actions
- GitHub Container Registry (GHCR)

## Local development

### 1. Start the backend stack on Windows

From the repository root:

```powershell
cd backend
.\start-all.bat
```

The launcher starts infrastructure and the IAM + HRMS JVM services in dependency-aware order and waits for health gates.

### 2. Start the Admin Portal

In a second terminal:

```powershell
cd frontend\admin-portal
npm ci
npm run dev
```

Use the URL printed by Vite. Local runtime configuration should use repository-provided development examples; do not commit personal or production secrets.

## Guided demo

A repository demo should show the product as a workflow rather than as a list of endpoints:

1. Admin login
2. Dashboard
3. Organization and user context
4. Role/permission enforcement
5. Employee + HR master data
6. Attendance / Leave
7. Salary structure + Payroll
8. Payslip
9. Sessions + Audit
10. Logout and revoked-token rejection

See [`docs/demo-guide.md`](docs/demo-guide.md) for the complete demo script and expected proof points.

## Production deployment baseline

The repository includes a V1 single-VM production baseline:

- `docker-compose.prod.yml`
- `deploy/production/.env.example`
- `deploy/production/Caddyfile`
- `deploy/production/README.md`
- `.github/workflows/publish-images.yml`
- MySQL backup/restore scripts under `scripts/production`

The intended initial topology is:

```text
Internet
   ↓
HTTPS / Caddy
   ↓
API Gateway
   ↓
Private Docker network
   ↓
IAM + HRMS services
   ↓
MySQL / Redis / RabbitMQ
```

Only the reverse-proxy entrypoint should be internet-facing for backend APIs. Internal service, Eureka, Config, MySQL, Redis and RabbitMQ ports stay private in the production topology.

For staging/production instructions, use [`deploy/production/README.md`](deploy/production/README.md).

## Repository status

The current codebase contains the IAM + HRMS product flow and a production deployment baseline. A real staging rollout is a separate operational validation step and should verify HTTPS, immutable GHCR images, fresh environment secrets, Flyway startup, health checks, the guided smoke flow, backup/restore and rollback before customer data is introduced.

## Future enhancements

These are roadmap items, not claims about the current release:

- OAuth 2.x / OpenID Connect provider capabilities
- SAML / enterprise SSO
- SCIM provisioning
- Passkeys / WebAuthn
- Advanced conditional access / risk policies
- Tenant self-service signup and subscription billing
- Expanded organization security policies
- Advanced invitation administration
- Managed observability stack
- Kubernetes only if scale/operations justify it

## Documentation

Useful references:

- [`docs/demo-guide.md`](docs/demo-guide.md) — guided product demo
- [`docs/architecture`](docs/architecture) — architecture documentation
- [`docs/admin-portal-api-contracts.md`](docs/admin-portal-api-contracts.md) — Admin Portal API contracts
- [`docs/asyncapi-events.yml`](docs/asyncapi-events.yml) — event contracts
- [`docs/database-migration-baseline.md`](docs/database-migration-baseline.md) — Flyway/database baseline
- [`docs/notification-integration.md`](docs/notification-integration.md) — notification integration
- [`deploy/production/README.md`](deploy/production/README.md) — production deployment runbook

---

UMS is being developed as a reusable identity/security foundation with HRMS as the first integrated business product. The goal is a demonstrable, secure and operationally repeatable platform rather than a collection of disconnected microservices.
