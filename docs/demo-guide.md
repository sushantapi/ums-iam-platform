# UMS IAM + HRMS Guided Demo

This guide is the recommended product demo path for reviewers, beta customers and portfolio walkthroughs.

The goal is to demonstrate one connected workflow across IAM, tenant context, HRMS operations, payroll and security controls rather than showing isolated endpoints.

## Demo principles

- Use a **sample organization and synthetic users only**.
- Do not use real employee, payroll, bank, PAN/UAN or customer data in a public demo.
- Do not expose production secrets, private JWT keys or internal service credentials.
- Keep the demo focused on visible business/security value.
- If a step is unavailable in the current environment, skip it rather than presenting a mock as real functionality.

## Recommended sample organization

Use a synthetic tenant such as:

```text
Organization: Acme Technologies Pvt Ltd
Admin: admin@acme-demo.example
Employee: employee@acme-demo.example
Department: Engineering
Designation: Backend Engineer
Employee Code: EMP-001
```

Use environment-specific credentials outside Git. Never commit demo passwords.

## Demo flow

### 1. Admin login

Show the Admin Portal login screen and authenticate with a seeded/bootstrap admin account.

**Proof points**

- valid login succeeds
- authenticated user lands on the protected dashboard
- protected pages are not available anonymously
- the browser/API client uses the real backend, not frontend mocks

### 2. Dashboard

Open the Dashboard and explain that it is the administrative overview for identity, organizations and operational activity.

**Proof points**

- dashboard loads through API Gateway
- displayed values come from backend services
- the session is authenticated and permission-aware

### 3. Organization context

Open Organizations and select the sample organization.

Show available organization/member administration surfaces.

**Message for the audience**

> UMS uses the organization as the tenant boundary. Identity and HRMS data are resolved inside that organization context.

### 4. User and access control

Open Users, then Roles / Permissions / Grants.

Show:

- user list/detail
- current roles/permissions
- role assignment where the environment allows it
- capability-based navigation/action visibility

**Security proof point**

A user without an administrative capability should receive a denied/forbidden result rather than inheriting access by default.

### 5. Tenant isolation

When two sample organizations are available, demonstrate that a user scoped to Organization A cannot administer Organization B resources unless explicitly authorized.

**Expected result**

```text
Allowed organization resource -> success
Cross-tenant unauthorized resource -> denied (403/404 according to the API contract)
```

This is one of the most important SaaS security demonstrations.

### 6. Company / HRMS setup

Open HRMS Company Settings and explain the relationship:

```text
UMS Organization
      ↓
HRMS company/payroll context
      ↓
Employees and HR operations
```

Do not show raw UUIDs unless needed for troubleshooting.

### 7. Employee management

Open Employees and the sample employee detail page.

Show available employee identity/master data plus Department and Designation master data.

**Message for the audience**

> HRMS reuses UMS identity and organization boundaries instead of creating a second authentication system.

### 8. Attendance

Open Attendance and show available attendance data/actions for the sample organization.

Keep this part short; the goal is to prove that the employee identity flows into HR operations.

### 9. Leave

Open Leave and demonstrate the available request/read/approval-oriented flow using synthetic data.

**Proof points**

- organization-scoped data
- permission-controlled operations
- status-driven leave workflow

### 10. Salary structure

Open Payroll and select the sample employee.

Show the current salary structure and, if safe in the environment, the salary revision/supersede flow.

**Important product message**

> Salary revisions create/version structures rather than overwriting historical compensation data.

Use simple demo values only.

### 11. Payroll run

Create or open a synthetic payroll run for the sample organization.

Show the progression supported by the current environment and the resulting payroll entries.

Mention the statutory fields available in the payroll model, including PF, ESI, TDS and tax regime where configured.

### 12. Payslip

Open the finalized payroll entry/payslip flow and download or preview the generated payslip where available.

**Proof points**

- payslip is based on persisted payroll data
- employee/payroll values match the finalized entry
- historical payroll data is not recomputed from a future salary structure

### 13. Sessions

Open Sessions and show the active administrative session.

If the demo environment allows a second test session, revoke it and explain immediate session/token invalidation.

### 14. Audit

Open Audit and locate activity generated during the demo, for example:

- login/security activity
- role assignment/revocation
- organization updates
- HRMS administrative actions where emitted

**Message for the audience**

> Security-sensitive administration should be traceable instead of being an invisible database change.

### 15. Logout / revocation

Logout from the Admin Portal.

For a technical demo, reuse the old access token against a protected API.

**Expected result**

```text
Before logout: protected API succeeds
Logout/revoke: succeeds
Old token reused: 401 Unauthorized
```

This closes the IAM security story.

## Short 5-minute demo version

When time is limited, use this path:

```text
Login
  ↓
Dashboard
  ↓
Organization
  ↓
Employee
  ↓
Role / Permission
  ↓
Payroll
  ↓
Payslip
  ↓
Audit
  ↓
Logout / revoked token
```

Suggested narration:

1. **One identity** — authentication and sessions are centralized.
2. **One tenant boundary** — organization context isolates customer data.
3. **Fine-grained authorization** — roles and permissions control administration.
4. **Connected HR operations** — the same identity is used for employee, attendance, leave and payroll.
5. **Auditable payroll/security** — salary history, payslips, sessions and audit are preserved instead of being disconnected modules.

## Portfolio / marketing screenshots

Capture screenshots only with synthetic data. Recommended screenshots:

1. Login
2. Dashboard
3. Users / Roles
4. Organization
5. Employees
6. Attendance or Leave
7. Payroll / Salary Structure
8. Payslip
9. Audit / Sessions

Avoid screenshots containing tokens, browser storage, private keys, internal secrets or personal customer information.

## Staging acceptance before sharing a live demo

Before publishing a live URL, verify:

- exact release/main SHA recorded
- frontend uses the HTTPS staging API and mocks are disabled
- HTTPS works end-to-end
- only intended public ports are exposed
- all expected JVM services and infrastructure are healthy
- Flyway migrations completed
- login/dashboard/HRMS/payslip/logout smoke passes
- revoked token is rejected
- fresh staging-only secrets are in use
- backup/restore and rollback procedures have been tested

Production/staging operations are documented in `deploy/production/README.md` and tracked separately from this demo script.
