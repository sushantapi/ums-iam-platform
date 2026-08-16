# Admin Portal Integration Checklist

This document is the source of truth for admin portal integration work. Complete one feature at a time, verify its backend contract before changing the frontend, and mark an item complete only after an end-to-end check.

## Global

- [x] Admin portal uses `runtimeConfig` with global and feature-level mock/real toggles
- [x] Fetch sends the JWT access token from `localStorage.ums_admin_access_token`
- [ ] 401 redirects to login
- [ ] 403 shows a forbidden state
- [ ] All list endpoints use the standard page contract
- [x] Backend CORS allows the admin portal origin

---

## Dashboard

- [x] `GET /api/v1/admin/dashboard`
- [x] Frontend dashboard page is bound to the real response contract
- [x] Mock fallback is available

## Sessions

- [x] `GET /api/v1/admin/sessions`
- [x] `GET /api/v1/admin/users/{userId}/sessions`
- [x] `POST /api/v1/admin/sessions/{sessionId}/revoke`
- [x] `POST /api/v1/admin/users/{userId}/sessions/revoke-all`
- [x] `SessionsPage` uses the real backend
- [x] `UserDetailPage` sessions tab uses the real backend

Implementation note: revocation disables refresh-token renewal immediately. Existing access JWTs remain valid until their configured expiry unless token-family validation is added to the shared security layer.

## Audit

- [x] `GET /api/v1/audit/events`
- [x] `GET /api/v1/audit/events/{eventId}`
- [x] `AuditLogsPage` uses the real backend
- [x] `AuditDetailPage` uses the real backend
- [x] User audit tab uses the real backend

Verified on June 22, 2026 through the API gateway with a local `SUPER_ADMIN` integration account: 23 events, list pagination, event detail, and user-target filtering.

## Roles / Permissions / Grants

- [ ] `GET /api/v1/admin/roles`
- [ ] `GET /api/v1/admin/roles/{roleId}`
- [ ] `GET /api/v1/admin/roles/{roleId}/permissions`
- [ ] `GET /api/v1/admin/permissions`
- [ ] `GET /api/v1/admin/grants`
- [ ] `DELETE /api/v1/admin/grants/{grantId}`
- [ ] `RolesPage` uses the real backend
- [ ] `RoleDetailPage` uses the real backend
- [ ] `PermissionsPage` uses the real backend
- [ ] `GrantsPage` uses the real backend

## Organizations

- [ ] `GET /api/v1/admin/organizations`
- [ ] `GET /api/v1/admin/organizations/{organizationId}`
- [ ] `GET /api/v1/admin/organizations/{organizationId}/members`
- [ ] `GET /api/v1/admin/organizations/{organizationId}/invitations`
- [ ] `GET /api/v1/admin/organizations/{organizationId}/security-policy`
- [ ] `PATCH /api/v1/admin/organizations/{organizationId}/security-policy`
- [ ] Organization pages use the real backend

## Users

- [ ] `GET /api/v1/admin/users`
- [ ] `GET /api/v1/admin/users/{userId}`
- [ ] `GET /api/v1/admin/users/{userId}/roles`
- [ ] `GET /api/v1/admin/users/{userId}/organizations`
- [ ] `GET /api/v1/admin/users/{userId}/sessions`
- [ ] `GET /api/v1/admin/users/{userId}/audit`
- [ ] User pages use the real backend

---

## Integration Workflow

Apply these steps to one feature before starting the next:

1. Confirm the backend endpoints and owning service.
2. Test the backend independently. Verify authentication, pagination, response fields, enum values, and date fields.
3. Align the types and methods in `frontend/admin-portal/src/lib/api.ts`.
4. Set that feature's `VITE_MOCK_*` flag to `false`.
5. Test list loading, filtering, details, mutations, error handling, and table refresh end to end.
6. Mark the verified checklist items complete.

## Integration Order

1. Dashboard
2. Sessions
3. Audit
4. Roles, permissions, and grants
5. Organizations
6. User 360

## Feature Ownership

| Feature | Primary backend owner | Frontend area |
| --- | --- | --- |
| Dashboard | `admin-service` | `src/features/dashboard` |
| Sessions | `authentication-service` or `admin-service` facade | `src/features/sessions`, user session tab |
| Audit | `audit-service` | `src/features/audit`, user audit tab |
| Roles / Permissions / Grants | `authorization-service` | `src/features/roles`, `permissions`, `grants` |
| Organizations | `organization-service` | `src/features/organizations` |
| Users | Multiple services through admin APIs | `src/features/users` |

## Mock Switches

`VITE_USE_MOCKS=false` disables every mock. When it is `true`, each `VITE_MOCK_*` flag controls one feature. A feature flag defaults to mocked unless explicitly set to `false`.

```dotenv
VITE_API_BASE_URL=http://localhost:8080
VITE_USE_MOCKS=true

VITE_MOCK_DASHBOARD=false
VITE_MOCK_USERS=true
VITE_MOCK_SESSIONS=false
VITE_MOCK_AUDIT=false
VITE_MOCK_ROLES=true
VITE_MOCK_PERMISSIONS=true
VITE_MOCK_GRANTS=true
VITE_MOCK_ORGANIZATIONS=true
```
