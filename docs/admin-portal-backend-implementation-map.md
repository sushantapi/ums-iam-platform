# Admin Portal Backend Implementation Map

This map connects `docs/admin-portal-api-contracts.md` to the UMS Spring Boot services.

## Implementation Order

1. Sessions admin APIs
2. Authorization admin APIs
3. Audit query/detail APIs
4. Organization security policy APIs
5. Dashboard aggregation API

## Authentication / Sessions

Primary owner: `authentication-service`

Admin facade option: `admin-service` may proxy these endpoints if the portal should call one admin BFF.

### Endpoints

- `GET /api/v1/admin/sessions?page=0&size=20&userId=&organizationId=&status=&from=&to=`
- `POST /api/v1/admin/sessions/{sessionId}/revoke`
- `POST /api/v1/admin/users/{userId}/sessions/revoke-all`

### DTOs

- `AdminSessionResponse`
  - `id`
  - `userId`
  - `userName`
  - `organizationId`
  - `organizationName`
  - `device`
  - `client`
  - `ipAddress`
  - `issuedAt`
  - `lastSeenAt`
  - `expiresAt`
  - `revokedAt`
  - `status`

### Service Methods

- `Page<AdminSessionResponse> listSessions(AdminSessionFilter filter, Pageable pageable)`
- `void revokeSession(UUID sessionId, AdminActor actor)`
- `void revokeAllUserSessions(UUID userId, AdminActor actor)`

### Rules

- Platform admins can revoke any session.
- Tenant admins can only revoke sessions in allowed organization scope.
- Revocation should invalidate refresh-token family state where available.
- Emit audit events for revoke-one and revoke-all actions.

## Authorization Admin

Primary owner: `authorization-service`

### Endpoints

- `GET /api/v1/admin/roles?page=0&size=20&scopeType=&search=`
- `GET /api/v1/admin/roles/{roleId}`
- `GET /api/v1/admin/roles/{roleId}/permissions`
- `GET /api/v1/admin/roles/{roleId}/assignments?page=0&size=20`
- `GET /api/v1/admin/permissions?page=0&size=20&search=`
- `GET /api/v1/admin/grants?page=0&size=20&userId=&roleId=&organizationId=`
- `DELETE /api/v1/admin/grants/{grantId}`

### DTOs

- `AdminRoleResponse`
- `AdminPermissionResponse`
- `AdminGrantResponse`

Use future-proof grant fields:

- `principalType`: `USER | GROUP | SERVICE_ACCOUNT`
- `principalId`
- `scopeType`: `PLATFORM | ORGANIZATION`
- `scopeId`
- `roleId`
- `assignedBy`
- `assignedAt`
- `status`
- `source`

### Service Methods

- `Page<AdminRoleResponse> listRoles(AdminRoleFilter filter, Pageable pageable)`
- `AdminRoleResponse getRole(UUID roleId)`
- `List<AdminPermissionResponse> getRolePermissions(UUID roleId)`
- `Page<AdminGrantResponse> getRoleAssignments(UUID roleId, Pageable pageable)`
- `Page<AdminPermissionResponse> listPermissions(AdminPermissionFilter filter, Pageable pageable)`
- `Page<AdminGrantResponse> listGrants(AdminGrantFilter filter, Pageable pageable)`
- `void revokeGrant(UUID grantId, AdminActor actor)`

## Audit Service

Primary owner: `audit-service`

### Endpoints

- `GET /api/v1/audit/events?page=0&size=50&actor=&target=&organizationId=&eventType=&outcome=&from=&to=`
- `GET /api/v1/audit/events/{eventId}`

### DTOs

- `AuditEventResponse`
  - `id`
  - `eventType`
  - `occurredAt`
  - `actor`
  - `target`
  - `organization`
  - `outcome`
  - `sourceService`
  - `summary`
  - `metadata`
  - `changedFields`
  - `correlationId`
  - `traceId`

### Service Methods

- `Page<AuditEventResponse> searchEvents(AuditEventFilter filter, Pageable pageable)`
- `AuditEventResponse getEvent(UUID eventId)`

## Organization Security Policy

Primary owner: `organization-service`

### Endpoints

- `GET /api/v1/admin/organizations/{organizationId}/security-policy`
- `PATCH /api/v1/admin/organizations/{organizationId}/security-policy`

### DTO

- `OrganizationSecurityPolicyResponse`
  - `mfaRequired`
  - `sessionIdleTimeoutMinutes`
  - `sessionAbsoluteTimeoutMinutes`
  - `passwordMinLength`
  - `inviteExpiryHours`
  - `allowMemberInvite`
  - `allowedDomains`
  - `inviteResendLimit`
  - `defaultInviteTemplate`
  - `auditSeverity`

### Service Methods

- `OrganizationSecurityPolicyResponse getSecurityPolicy(UUID organizationId, AdminActor actor)`
- `OrganizationSecurityPolicyResponse updateSecurityPolicy(UUID organizationId, UpdateSecurityPolicyRequest request, AdminActor actor)`

## Dashboard Aggregation

Primary owner: `admin-service`

### Endpoint

- `GET /api/v1/admin/dashboard`

### Aggregates

- user totals by status
- organization totals
- active sessions
- recent audit count
- grants count
- locked accounts
- pending invitations

## Standard Pagination Contract

All admin list endpoints should return:

- `content`
- `page`
- `size`
- `totalElements`
- `totalPages`
