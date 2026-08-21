# UMS IAM + HRMS Basic v1 Architecture Checkpoint

## Status

This document describes the architecture actually shipped in the combined UMS IAM Basic v1 + HRMS Basic v1 baseline. It is a release checkpoint, not a future-state design.

The canonical combined checkpoint is `v1.1.0`. The earlier `v1.0.0` tag is retained as immutable historical release history after the combined-release version labeling was corrected. Basic v1 remains frozen except for genuine bug fixes; post-v1 capabilities must be tracked separately.

## System context

```text
Browser
  |
  | HTTPS / REST
  v
React Admin Portal
  |
  | IAM access token + organization context
  v
API Gateway
  |
  +---------------- IAM ----------------+
  |                                     |
  |  authentication-service             |
  |  user-service                       |
  |  authorization-service              |
  |  organization-service               |
  |  admin-service                      |
  |  audit-service                      |
  |  notification-service               |
  |                                     |
  +--------------- HRMS ----------------+
  |                                     |
  |  employee-service                   |
  |  attendance-service                 |
  |  leave-service                      |
  |  payroll-service                    |
  |                                     |
  +-------------------------------------+
            |                    |
            v                    v
      Eureka Discovery       Config Server
                                  |
                                  v
                           backend/config-repo

Service-owned persistence      RabbitMQ
(MySQL + Flyway)                  |
                                  v
                         async/audit integration
                         where currently implemented
```

## Repository runtime modules

The backend Maven reactor contains the following deployable services and shared modules:

### Platform and IAM

- `config-service` — centralized runtime configuration
- `discovery-service` — Eureka service discovery
- `api-gateway` — browser-facing routing and trusted backend boundary
- `authentication-service` — login, token/session authentication flows
- `user-service` — user lifecycle and user data
- `authorization-service` — roles, permissions, grants, and authorization data
- `organization-service` — organization/tenant ownership and membership data
- `audit-service` — audit/event visibility
- `notification-service` — notification integration
- `admin-service` — IAM admin/dashboard aggregation capabilities

### HRMS

- `employee-service` — employee lifecycle plus department/designation master data
- `attendance-service` — tenant-scoped attendance records
- `leave-service` — leave requests and state transitions
- `payroll-service` — salary structures, payroll runs, entries, and payslip snapshots

### Shared backend modules

- `common/common-security` — shared security primitives used by backend services
- `common/common-events` — shared event contracts used by asynchronous integrations

## Frontend boundary

The shipped frontend is the existing React/Vite application at `frontend/admin-portal`.

It contains both IAM administration and HRMS administration. HRMS Basic v1 adds routes/navigation for:

- Employees
- Departments
- Designations
- Attendance
- Leave
- Payroll

The browser communicates with backend business APIs through the API Gateway. Direct browser calls to internal service ports are not part of the supported architecture.

Frontend capability checks are for user experience only. Backend services remain authoritative for authentication, authorization, organization access, and tenant isolation.

## Identity and authorization model

HRMS does not maintain a second identity system.

The HRMS flow reuses the existing IAM:

- login/session
- access token/JWT
- immutable UMS user identity
- organization/tenant context
- roles and canonical permissions/authorities

An HRMS employee links to the existing UMS user identity instead of duplicating passwords, sessions, or authentication state.

The API Gateway validates the external authentication boundary and forwards trusted identity context according to the current gateway security implementation. Downstream services still enforce their own business authorization and tenant rules.

## Service and database ownership

Basic v1 follows database-per-service ownership.

Each business service owns its persistence model and migrations. Services must not couple through cross-service SQL joins or cross-service database foreign keys.

Cross-service validation is performed through service contracts. Examples include validating:

- organization access/membership
- employee existence and ACTIVE state
- tenant ownership of referenced data

Flyway is the schema migration mechanism where service database schema evolution is required. Historical migrations are immutable after release; subsequent schema changes use new versioned migrations.

## HRMS boundaries

### Employee and organization structure

`employee-service` owns employee records, departments, designations, employment status, and the link to immutable IAM user identity.

### Attendance

`attendance-service` owns attendance records. Employee references are validated through service contracts rather than database sharing.

### Leave

`leave-service` owns leave requests and transitions such as pending, approved, rejected, and cancelled. Authorization for request/approval/cancellation is based on canonical IAM permissions.

### Payroll

`payroll-service` owns salary structures, payroll runs, generated payroll entries, and payslip snapshots.

The Basic v1 payroll lifecycle is:

```text
DRAFT -> PROCESSED -> FINALIZED
```

The backend is the source of truth for monetary values such as gross, deductions, and net pay. The frontend displays backend-returned values and does not independently recalculate payroll as authoritative state.

## Discovery and configuration

Services use Eureka for service registration/discovery and Config Server for centralized environment-specific configuration.

Docker runtime configuration is served from `backend/config-repo` using the Docker profile. Container-to-container service discovery must use Docker/Eureka service names rather than host-local addresses.

## Messaging and audit integration

RabbitMQ is part of the current platform runtime for asynchronous/event-driven integration points.

Where lifecycle events are implemented, services publish shared event contracts and the audit path records relevant business activity. Event payloads should carry identifiers and lifecycle context without turning the event bus into a shared database.

RabbitMQ is not a replacement for synchronous ownership validation. Requests that must confirm current ownership/state use the appropriate service contract.

## Saga policy

Saga/orchestration is intentionally not used for ordinary Basic v1 CRUD operations.

A Saga should be introduced only when a future business workflow genuinely spans multiple independently committed service states and needs compensating actions or coordinated distributed state transitions.

Examples of future candidates may include complex payroll payment execution or other multi-service workflows, but those are explicitly outside Basic v1.

## Release verification boundary

The canonical `v1.1.0` combined release checkpoint requires:

- Backend CI green
- Frontend CI green
- frontend tests, production build, and lint green
- real Docker/API Gateway runtime smoke green
- IAM login/session reused by HRMS
- tenant-scoped employee/org-structure reads verified
- attendance create/update verified
- leave create and permitted transition verified
- payroll create/process/entry/payslip/finalize verified
- unauthenticated HRMS Gateway calls rejected
- IAM admin/dashboard behavior verified without regression

The v1.1.0 correction itself changes only release/workflow/documentation metadata; it does not alter the shipped application architecture or behavior.

This document should be updated only when the shipped architecture changes; aspirational post-v1 capabilities belong in roadmap/backlog documentation instead.
