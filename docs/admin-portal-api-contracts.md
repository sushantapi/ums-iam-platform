# Admin Portal API Contracts

These contracts are the stable frontend target for the four live IAM admin screens.

## Dashboard

`GET /api/v1/admin/dashboard`

```json
{
  "users": {
    "total": 1200,
    "active": 1100,
    "locked": 8,
    "suspended": 12
  },
  "organizations": {
    "total": 45,
    "active": 41,
    "pendingInvitations": 6
  },
  "roles": {
    "total": 28
  },
  "audit": {
    "eventsLast24Hours": 560,
    "failedLogins": 14
  }
}
```

## Users List

`GET /api/v1/admin/users?page=0&size=20&search=&status=&organizationId=&role=`

```json
{
  "content": [
    {
      "id": "uuid",
      "fullName": "Sushant Kumar",
      "email": "test@demo.com",
      "status": "ACTIVE",
      "organizationName": "Acme",
      "roles": ["ORG_ADMIN"],
      "lastLoginAt": "2026-06-21T09:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 120,
  "totalPages": 6
}
```

## User 360

- `GET /api/v1/admin/users/{userId}`
- `GET /api/v1/admin/users/{userId}/roles`
- `GET /api/v1/admin/users/{userId}/organizations`
- `GET /api/v1/admin/users/{userId}/sessions`
- `GET /api/v1/admin/users/{userId}/audit?page=0&size=25`

## Role Assignment

- `GET /api/v1/roles`
- `GET /api/v1/roles/{roleId}`
- `GET /api/v1/roles/{roleId}/permissions`
- `POST /api/v1/admin/roles/assign`
- `DELETE /api/v1/authorization/users/{userId}/roles/{roleId}`

## Audit Logs

`GET /api/v1/audit/events?page=0&size=50&actor=&target=&eventType=&organizationId=&from=&to=&outcome=`

Every audit row should include:

```json
{
  "eventId": "uuid",
  "eventType": "authorization.grant.created",
  "actor": "admin@example.com",
  "target": "user@example.com",
  "organizationId": "uuid",
  "organization": "Acme",
  "outcome": "SUCCESS",
  "ipAddress": "127.0.0.1",
  "serviceName": "authorization-service",
  "createdAt": "2026-06-21T09:00:00",
  "details": "Role ORG_ADMIN assigned"
}
```

## Sessions / Security Operations

- `GET /api/v1/admin/sessions?page=0&size=20&userId=&organizationId=&status=&from=&to=`
- `POST /api/v1/admin/sessions/{sessionId}/revoke`
- `POST /api/v1/admin/users/{userId}/sessions/revoke-all`

Every session row should include `id`, `userId`, `userName`, `organizationId`,
`organizationName`, `device`, `client`, `issuedAt`, `lastSeenAt`, `expiresAt`,
`status`, and `ipAddress` where available.

## Authorization Admin

- `GET /api/v1/admin/permissions?page=0&size=20&search=`
- `GET /api/v1/admin/roles?page=0&size=20&scopeType=&search=`
- `GET /api/v1/admin/roles/{roleId}`
- `GET /api/v1/admin/roles/{roleId}/permissions`
- `GET /api/v1/admin/roles/{roleId}/assignments?page=0&size=20`
- `GET /api/v1/admin/grants?page=0&size=20&userId=&roleId=&organizationId=`
- `DELETE /api/v1/admin/grants/{grantId}`

Permission rows should include `code`, `resource`, `action`, `description`,
`systemPermission`, and `rolesUsingPermission`.

Grant rows should include `principal`, `principalType`, `roleName`,
`organizationId`, `organizationName`, `scope`, `assignedBy`, `assignedAt`,
`status`, and `source`.

## Organization Security Policy

- `GET /api/v1/admin/organizations/{organizationId}/security-policy`
- `PATCH /api/v1/admin/organizations/{organizationId}/security-policy`

Policy fields should cover `mfaRequired`, `sessionTimeoutMinutes`,
`passwordPolicyRef`, `inviteExpiryHours`, `invitedByRoles`,
`roleAssignmentRoles`, `selfServiceJoinEnabled`, `inviteResendLimit`,
`defaultInviteTemplate`, and `auditSeverity`.

## Audit Detail

`GET /api/v1/audit/events/{eventId}`

Audit detail should include event metadata, actor, target, organization, outcome,
request context, changed field summary, correlation ID, and trace ID when available.

## Phase 3 Admin Route Map

- `/dashboard`
- `/users`
- `/users/:userId`
- `/organizations`
- `/organizations/:organizationId`
- `/organizations/:organizationId/members`
- `/organizations/:organizationId/invitations`
- `/organizations/:organizationId/security`
- `/roles`
- `/roles/:roleId`
- `/roles/assignments`
- `/permissions`
- `/grants`
- `/audit`
- `/audit/:eventId`
- `/operations/sessions`
