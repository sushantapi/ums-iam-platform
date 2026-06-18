# UMS IAM Platform - Complete Enterprise IAM Implementation Plan

Review date: 2026-06-12

## 1. Current State

The repository currently provides partial implementations of:

- Authentication: registration, password login, JWT access/refresh tokens, session
  records, login lockout, and local authentication audit rows.
- Authorization: roles, permissions, role-permission mappings, user-role assignments,
  and permission lookup.
- User: profile and preference storage.
- Organization: organizations and direct organization membership.
- Notification: templates, email delivery, delivery logs, and retry records.
- Gateway: custom JWT signature validation and service discovery routing.
- Config and Discovery: Spring Cloud Config and Eureka.
- Audit: referenced by admin-service but the actual `c` is empty.

This is an IAM foundation prototype. It does not yet implement the protocols, tenant
controls, lifecycle management, governance, evidence, and operational controls expected
from an enterprise IAM platform.

## 2. Target Service Ownership

| Service | Authoritative responsibility |
|---|---|
| authentication-service | Credentials, authenticators, login ceremonies, OAuth/OIDC authorization server, tokens, sessions, recovery, federation |
| authorization-service | Tenant-scoped roles, permissions, groups, grants, policies, authorization decisions, access reviews |
| user-service | User profile, lifecycle state, enterprise attributes, linked identities, privacy preferences |
| organization-service | Tenants, domains, invitations, memberships, organization hierarchy, tenant settings |
| notification-service | Security and lifecycle notifications, templates, preferences, delivery attempts |
| audit-service | Immutable security and administration events, search, export, retention, SIEM delivery |
| api-gateway | Edge authentication enforcement, route allowlist, rate limits, security headers, request identity context |
| config-service | Non-secret runtime configuration and feature flags |
| discovery-service | Internal service registration and discovery only |

### Boundary decisions

- Remove authoritative roles from authentication-service. Tokens may contain a
  short-lived entitlement snapshot, but authorization-service owns the source data.
- Do not let user-service or organization-service implement independent permission
  systems.
- Do not let audit-service participate in synchronous business transactions.
- Do not use Config Server to distribute private keys or production credentials.
- Do not expose Discovery or Config Server through the public gateway.

## 3. Missing Enterprise IAM Features

## 3.1 Authentication and Identity Protocols

### Required

- OAuth 2.1-style authorization code flow with PKCE.
- OpenID Connect discovery, authorization, token, user-info, logout, and JWKS endpoints.
- Client credentials for service accounts.
- Refresh-token rotation with token-family reuse detection.
- Public and confidential client registration.
- Signed ID tokens with `nonce`, `auth_time`, `acr`, and `amr`.
- Token revocation and token introspection for approved confidential clients.
- Key IDs and zero-downtime signing-key rotation.
- Audience-restricted access tokens.
- Step-up authentication for high-risk operations.

### Enterprise federation

- External OIDC identity provider connections.
- SAML 2.0 service-provider support.
- Tenant-specific identity provider routing.
- Just-in-time user provisioning.
- Account linking with takeover-resistant verification.
- Domain-based home realm discovery.
- IdP certificate and metadata rotation.

### Authenticators

- TOTP enrollment and verification.
- WebAuthn/passkeys for phishing-resistant authentication.
- Recovery codes stored as one-way hashes.
- Authenticator naming, last-used time, and revocation.
- MFA enrollment and recovery policy.
- Optional email/SMS OTP only as lower-assurance recovery mechanisms.
- Risk-based challenge escalation.

### Password lifecycle

- Verified email before account activation.
- Forgot-password and reset-password flows.
- Password-change flow requiring current password or step-up authentication.
- Password history and breached-password screening.
- Configurable tenant password policy.
- Force reset and credential expiration only where organizational policy requires it.
- Generic responses and throttling to prevent account enumeration.

### Session and device management

- List current sessions and devices.
- Revoke one session or all sessions.
- Track refresh-token families and rotation history.
- Session idle and absolute timeout.
- Device recognition and new-device notifications.
- Session risk score and authentication context.
- Forced revocation after password, authenticator, account, or entitlement changes.

## 3.2 User Lifecycle and Directory

- User states: invited, pending verification, active, suspended, locked, disabled,
  deprovisioned, and deleted.
- Administrative create, activate, suspend, unlock, disable, restore, and deprovision.
- Manager, department, employee ID, cost center, location, title, and employment status.
- Multiple verified emails and phone numbers.
- Immutable internal subject ID separated from mutable login identifiers.
- Identity aliases and linked external identities.
- User schema extension attributes.
- Bulk import with validation and dry-run.
- SCIM 2.0 Users and Groups APIs.
- Deprovisioning workflow that revokes sessions, memberships, grants, and credentials.
- Data export, correction, retention, and erasure workflows.

## 3.3 Authorization

### Required authorization models

- Platform roles for global operators.
- Tenant roles for organization administrators and users.
- Application roles for client/application-specific access.
- Permissions represented as stable `resource:action` identifiers.
- Groups and nested group membership with cycle prevention.
- Direct user grants and group-derived grants.
- Scope and resource constraints on grants.
- Grant start/end time and revocation reason.
- Deny-by-default policy.

### Fine-grained controls

- Resource ownership checks.
- Tenant isolation checks on every tenant-owned resource.
- Attribute-based policy conditions.
- Relationship-based checks where manager/team/resource relationships matter.
- Delegated administration with constrained scope.
- Separation-of-duties rules.
- Break-glass roles with approval, expiration, and enhanced audit.
- Cached authorization decisions with bounded TTL and entitlement version invalidation.
- Explainable decisions that record matched policy and reason.

### Governance

- Access request and approval workflow.
- Time-bound privileged access.
- Periodic access certification.
- Manager and application-owner review campaigns.
- Orphaned account and excessive privilege detection.
- Inactive account review.
- Role mining and unused permission reporting.

## 3.4 Organization and Multi-Tenancy

- Tenant-level immutable ID in all tenant-owned tables and events.
- Organization domain verification.
- Invitation issue, resend, accept, decline, expire, and revoke.
- Membership state and lifecycle.
- Organization teams, departments, and hierarchy.
- Tenant security policy, session policy, MFA policy, and password policy.
- Tenant branding and notification settings.
- Tenant-specific identity providers and client applications.
- Tenant suspension and deletion workflow.
- Tenant data export and retention policy.
- Tenant quotas and service-plan limits.
- Cross-tenant administration restricted to platform operators.

## 3.5 Provisioning and Integration

- SCIM 2.0 Users and Groups resources.
- SCIM service-provider configuration, resource types, and schemas.
- Bulk provisioning and cursor-based pagination.
- Inbound and outbound provisioning jobs.
- Webhook subscriptions with signing and replay protection.
- Service accounts and machine identities.
- API clients with scoped credentials and rotation.
- Optional OAuth token exchange for trusted service delegation.
- Event schema registry/versioning and compatibility policy.

## 3.6 Audit, Reporting, and Compliance

- Central append-only audit service.
- Tamper-evident event chaining or immutable/WORM archive.
- Actor, subject, tenant, resource, outcome, reason, IP, device, user agent, trace, and
  request identifiers.
- Before/after change summaries with sensitive-field redaction.
- Search by time, tenant, actor, target, event type, outcome, IP, and correlation ID.
- Signed export and SIEM streaming.
- Configurable retention and legal hold.
- User-visible security history.
- Privileged administrator activity reports.
- Authentication, entitlement, and provisioning reports.

## 4. Missing Database Tables

All tables must use Flyway migrations. Production Hibernate schema handling should be
`validate`, not `update`.

## 4.1 authentication-service Database

### Core identity and credentials

| Table | Purpose | Important columns and constraints |
|---|---|---|
| `auth_users` | Authentication subject and login state | `id`, `primary_login`, `status`, `credential_version`, `entitlement_version`, timestamps; unique normalized login |
| `user_identifiers` | Emails, phones, usernames, aliases | `user_id`, `type`, `normalized_value`, `verified_at`, `primary_flag`; unique `(type, normalized_value)` |
| `password_credentials` | Current password metadata | `user_id`, `password_hash`, `changed_at`, `must_change`, `algorithm`, `parameters` |
| `password_history` | Password reuse prevention | `user_id`, `password_hash`, `created_at`; index by user/time |
| `external_identities` | OIDC/SAML account links | `user_id`, `provider_id`, `issuer`, `external_subject`; unique `(provider_id, external_subject)` |
| `authenticators` | Common authenticator record | `id`, `user_id`, `type`, `name`, `status`, `assurance_level`, `last_used_at` |
| `totp_credentials` | TOTP secret reference and counters | `authenticator_id`, encrypted secret reference, digits, period, algorithm |
| `webauthn_credentials` | Passkeys/security keys | credential ID, public key, sign count, transports, AAGUID; unique credential ID |
| `recovery_codes` | One-time recovery codes | `user_id`, `code_hash`, `used_at`; never store plaintext |

### Verification and recovery

| Table | Purpose |
|---|---|
| `verification_challenges` | Email/phone verification challenges with hash, expiry, attempts, and consumed time |
| `password_reset_challenges` | Single-use reset token hashes, expiry, attempts, and consumption |
| `authentication_challenges` | MFA and step-up transaction state |
| `account_lockouts` | Lockout reason, source, start, expiry, and administrative release |
| `risk_signals` | Authentication risk inputs and calculated score |

### OAuth/OIDC

| Table | Purpose |
|---|---|
| `oauth_clients` | Client ID, tenant, type, status, grant types, auth method, consent policy |
| `oauth_client_secrets` | Hashed client secrets with expiry and rotation state |
| `oauth_redirect_uris` | Exact registered redirect and post-logout URIs |
| `oauth_client_scopes` | Scopes allowed for a client |
| `oauth_authorizations` | Authorization code and consent transaction state |
| `oauth_consents` | User-client granted scopes and timestamps |
| `signing_keys` | `kid`, algorithm, public material/reference, activation, retirement, status |
| `federation_providers` | Tenant OIDC/SAML provider configuration |
| `federation_domains` | Verified domain to provider routing |

### Sessions and tokens

| Table | Purpose |
|---|---|
| `sessions` | Browser/device session, user, tenant, auth context, idle/absolute expiry, revoked state |
| `session_devices` | Device fingerprint metadata and trust state |
| `refresh_token_families` | Session token family, current generation, reuse/revocation state |
| `refresh_tokens` | One-time refresh token hash, parent, issued/used/revoked timestamps |
| `revoked_token_ids` | Exceptional short-lived access-token revocation by `jti` |
| `login_attempts` | Success/failure telemetry for throttling and investigation |

### Required changes to existing tables

- Migrate existing `users` into `auth_users` and identifiers/credential tables.
- Remove the authentication-service `roles` and `user_roles` ownership.
- Replace the current single refresh hash in `sessions` with refresh-token families.
- Move local `audit_logs` into audit-service after reliable event publishing is active.

## 4.2 authorization-service Database

| Table | Purpose | Important constraints |
|---|---|---|
| `permissions` | Stable resource/action permissions | unique `(resource, action)` and immutable permission code |
| `roles` | Platform, tenant, or application roles | unique `(tenant_id, application_id, normalized_name)` |
| `role_permissions` | Permission membership | unique `(role_id, permission_id)` |
| `groups` | Tenant directory groups | unique `(tenant_id, normalized_name)` |
| `group_memberships` | User/group membership | unique member edge; prevent nested cycles |
| `role_grants` | Role assignment to user/group/service account | tenant, principal type/id, scope, dates, grantor, status |
| `permission_grants` | Exceptional direct permission grants | same scope and expiry controls as role grants |
| `policy_sets` | Versioned policy container | tenant/application/status/version |
| `policies` | ABAC/ReBAC rules | effect, resource, action, condition document, priority |
| `resource_relations` | Subject-resource relationships | unique tenant/resource/relation/subject edge |
| `authorization_versions` | Entitlement version per principal | increment on relevant change |
| `delegated_admin_scopes` | Administrative scope boundaries | principal, allowed operations, target scope, expiry |
| `separation_of_duties_rules` | Conflicting role/permission combinations | unique rule code |
| `access_requests` | Requested access and justification | status and requester/beneficiary |
| `access_request_steps` | Approval workflow decisions | ordered approvers and outcomes |
| `access_review_campaigns` | Certification campaign definition | scope, owner, due date, status |
| `access_review_items` | Individual review decision | reviewer, principal, entitlement, decision |
| `break_glass_grants` | Emergency privileged access | approval, reason, expiry, revocation |

### Existing table changes

- Add `tenant_id`, optional `application_id`, status, version, and lifecycle timestamps.
- Replace `user_roles` with generalized `role_grants`.
- Add indexes beginning with `tenant_id` for every tenant query.
- Preserve historical grants rather than deleting them.

## 4.3 user-service Database

| Table | Purpose |
|---|---|
| `user_profiles` | Display and contact profile keyed by immutable auth subject |
| `user_employment_attributes` | Employee ID, manager, department, title, location, status |
| `user_addresses` | Multiple structured addresses |
| `user_phones` | Multiple phones and verification projection |
| `user_preferences` | Locale, timezone, theme, accessibility and communication choices |
| `user_custom_attributes` | Tenant-defined schema extension values |
| `user_lifecycle_history` | State transitions and reasons |
| `user_privacy_requests` | Export, correction, restriction, and erasure workflows |
| `user_import_jobs` | Bulk import status, source, counts, and validation results |
| `user_import_errors` | Row-level import failures |

### Constraints

- Unique `user_id` in preferences.
- Tenant-qualified uniqueness for employee ID.
- Manager references must not create hierarchy cycles.
- Encrypt sensitive attributes and keep search projections separate where needed.

## 4.4 organization-service Database

| Table | Purpose |
|---|---|
| `organizations` | Tenant record and lifecycle |
| `organization_domains` | Domain ownership and verification |
| `organization_memberships` | User membership, status, dates, inviter |
| `organization_invitations` | Hashed invitation token, target email, role, expiry |
| `organization_units` | Departments/teams/hierarchy |
| `organization_unit_memberships` | User membership in units |
| `organization_settings` | General tenant settings |
| `organization_security_policies` | MFA, session, password, federation, and risk policy |
| `organization_branding` | Logo, colors, sender names, hosted-login settings |
| `organization_entitlements` | Plan, feature, and quota assignments |
| `organization_identity_providers` | Tenant/provider association and routing |
| `organization_clients` | Tenant/application client association |
| `organization_lifecycle_history` | Suspend, restore, close, and delete transitions |

### Constraints

- Unique normalized slug.
- Unique verified domain globally unless explicit shared-domain policy exists.
- Unique `(organization_id, user_id)` membership.
- Hierarchy parent must belong to the same organization and must not form cycles.

## 4.5 notification-service Database

| Table | Purpose |
|---|---|
| `notification_templates` | Versioned template metadata |
| `notification_template_versions` | Immutable localized subject/body versions |
| `notification_preferences` | Per-user and per-tenant channel preferences |
| `notification_requests` | Idempotent request record |
| `notification_deliveries` | Per-channel delivery attempt and provider response |
| `notification_suppressions` | Bounces, complaints, unsubscribe, and administrator blocks |
| `notification_providers` | Provider configuration references and status |
| `webhook_subscriptions` | Tenant event callback configuration |
| `webhook_secrets` | Rotatable signing-secret hashes/references |
| `webhook_deliveries` | Attempt, response, retry, and dead-letter state |

## 4.6 audit-service Database

| Table | Purpose |
|---|---|
| `audit_events` | Append-only canonical event record |
| `audit_event_details` | Optional redacted change summary and structured metadata |
| `audit_hash_chain` | Previous/current hash or signed batch proof |
| `audit_exports` | Export request, filter, requester, checksum, expiry |
| `audit_retention_policies` | Tenant and event-category retention |
| `audit_legal_holds` | Hold scope and approval |
| `audit_siem_destinations` | SIEM integration metadata and secret reference |
| `audit_delivery_offsets` | Reliable downstream delivery checkpoint |
| `audit_archive_batches` | Immutable archive object, checksum, range, and status |

### Canonical `audit_events` fields

`event_id`, `occurred_at`, `received_at`, `event_type`, `event_version`,
`category`, `severity`, `outcome`, `reason_code`, `actor_type`, `actor_id`,
`actor_display`, `subject_type`, `subject_id`, `tenant_id`, `resource_type`,
`resource_id`, `action`, `source_service`, `source_ip`, `user_agent`,
`device_id`, `session_id`, `client_id`, `request_id`, `trace_id`,
`correlation_id`, `causation_id`, `auth_context`, `data_classification`,
`change_summary`, and `integrity_hash`.

Do not store passwords, tokens, OTP values, private keys, secrets, or full sensitive
attribute values in audit records.

## 4.7 Shared Reliability Tables

Each state-changing service should have:

- `outbox_events`: event ID, aggregate, event type/version, payload, created/published
  timestamps, attempts, and error.
- `inbox_events`: consumer, event ID, received/processed timestamps, outcome.
- `idempotency_keys`: tenant, caller, operation, key, request hash, response reference,
  expiry.

## 5. Missing APIs

Paths below are recommended target contracts. Public APIs should use consistent
versioning and RFC 9457 Problem Details.

## 5.1 Authentication APIs

### Account and password

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/email-verifications`
- `POST /api/v1/auth/email-verifications/{challengeId}/confirm`
- `POST /api/v1/auth/password-resets`
- `POST /api/v1/auth/password-resets/{challengeId}/confirm`
- `POST /api/v1/auth/password/change`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/logout-all`

### MFA and authenticators

- `GET /api/v1/auth/authenticators`
- `POST /api/v1/auth/authenticators/totp/enroll`
- `POST /api/v1/auth/authenticators/totp/confirm`
- `POST /api/v1/auth/authenticators/webauthn/options`
- `POST /api/v1/auth/authenticators/webauthn`
- `DELETE /api/v1/auth/authenticators/{authenticatorId}`
- `POST /api/v1/auth/recovery-codes/regenerate`
- `POST /api/v1/auth/challenges/{challengeId}/verify`

### Session and device

- `GET /api/v1/auth/sessions`
- `GET /api/v1/auth/sessions/current`
- `DELETE /api/v1/auth/sessions/{sessionId}`
- `DELETE /api/v1/auth/sessions`
- `GET /api/v1/auth/devices`
- `PATCH /api/v1/auth/devices/{deviceId}/trust`
- `DELETE /api/v1/auth/devices/{deviceId}`

### OAuth/OIDC protocol

- `GET /.well-known/openid-configuration`
- `GET /.well-known/oauth-authorization-server`
- `GET /oauth2/jwks`
- `GET|POST /oauth2/authorize`
- `POST /oauth2/token`
- `POST /oauth2/revoke`
- `POST /oauth2/introspect`
- `GET|POST /oauth2/userinfo`
- `GET|POST /connect/logout`
- `GET /oauth2/consents`
- `DELETE /oauth2/consents/{clientId}`

### Client administration

- `POST /api/v1/oauth-clients`
- `GET /api/v1/oauth-clients`
- `GET /api/v1/oauth-clients/{clientId}`
- `PATCH /api/v1/oauth-clients/{clientId}`
- `POST /api/v1/oauth-clients/{clientId}/secrets`
- `DELETE /api/v1/oauth-clients/{clientId}/secrets/{secretId}`
- `POST /api/v1/oauth-clients/{clientId}/rotate-secret`

### Federation

- `POST /api/v1/federation/providers`
- `GET /api/v1/federation/providers`
- `PATCH /api/v1/federation/providers/{providerId}`
- `POST /api/v1/federation/providers/{providerId}/test`
- `POST /api/v1/federation/providers/{providerId}/rotate`
- `DELETE /api/v1/federation/providers/{providerId}`

## 5.2 User APIs

- `GET /api/v1/users/me`
- `PATCH /api/v1/users/me/profile`
- `GET /api/v1/users/me/security-history`
- `GET /api/v1/users?cursor=&limit=&status=&search=`
- `POST /api/v1/users`
- `GET /api/v1/users/{userId}`
- `PATCH /api/v1/users/{userId}`
- `POST /api/v1/users/{userId}/activate`
- `POST /api/v1/users/{userId}/suspend`
- `POST /api/v1/users/{userId}/unlock`
- `POST /api/v1/users/{userId}/disable`
- `POST /api/v1/users/{userId}/deprovision`
- `POST /api/v1/users/{userId}/restore`
- `GET /api/v1/users/{userId}/identities`
- `POST /api/v1/users/{userId}/identities/link`
- `DELETE /api/v1/users/{userId}/identities/{identityId}`
- `POST /api/v1/user-imports`
- `GET /api/v1/user-imports/{jobId}`
- `POST /api/v1/users/{userId}/data-export`
- `POST /api/v1/users/{userId}/erasure-request`

## 5.3 SCIM APIs

- `GET /scim/v2/ServiceProviderConfig`
- `GET /scim/v2/ResourceTypes`
- `GET /scim/v2/Schemas`
- `POST|GET /scim/v2/Users`
- `GET|PUT|PATCH|DELETE /scim/v2/Users/{id}`
- `POST|GET /scim/v2/Groups`
- `GET|PUT|PATCH|DELETE /scim/v2/Groups/{id}`
- `POST /scim/v2/Bulk`

SCIM endpoints need tenant-bound bearer credentials, filtering, ETags, pagination,
bulk limits, idempotency, and complete provisioning audit events.

## 5.4 Organization APIs

- `POST /api/v1/organizations`
- `GET /api/v1/organizations`
- `GET /api/v1/organizations/{organizationId}`
- `PATCH /api/v1/organizations/{organizationId}`
- `POST /api/v1/organizations/{organizationId}/suspend`
- `POST /api/v1/organizations/{organizationId}/restore`
- `DELETE /api/v1/organizations/{organizationId}`
- `GET /api/v1/organizations/{organizationId}/members`
- `PATCH /api/v1/organizations/{organizationId}/members/{userId}`
- `DELETE /api/v1/organizations/{organizationId}/members/{userId}`
- `POST /api/v1/organizations/{organizationId}/invitations`
- `GET /api/v1/organizations/{organizationId}/invitations`
- `POST /api/v1/organizations/{organizationId}/invitations/{invitationId}/resend`
- `DELETE /api/v1/organizations/{organizationId}/invitations/{invitationId}`
- `POST /api/v1/invitations/{token}/accept`
- `POST /api/v1/invitations/{token}/decline`
- `GET /api/v1/organizations/{organizationId}/domains`
- `POST /api/v1/organizations/{organizationId}/domains`
- `POST /api/v1/organizations/{organizationId}/domains/{domainId}/verify`
- `DELETE /api/v1/organizations/{organizationId}/domains/{domainId}`
- `GET|PATCH /api/v1/organizations/{organizationId}/security-policy`
- `GET|PATCH /api/v1/organizations/{organizationId}/settings`
- CRUD `/api/v1/organizations/{organizationId}/units`

## 5.5 Authorization APIs

### Roles, permissions, groups, and grants

- CRUD `/api/v1/permissions`
- CRUD `/api/v1/roles`
- `PUT /api/v1/roles/{roleId}/permissions`
- CRUD `/api/v1/groups`
- `PUT /api/v1/groups/{groupId}/members`
- `POST /api/v1/grants`
- `GET /api/v1/grants?principalId=&roleId=&tenantId=`
- `DELETE /api/v1/grants/{grantId}`
- `POST /api/v1/grants/{grantId}/extend`

### Decisions

- `POST /api/v1/authorization/check`
- `POST /api/v1/authorization/batch-check`
- `POST /api/v1/authorization/explain`
- `GET /api/v1/principals/{principalId}/entitlements`
- `GET /api/v1/principals/{principalId}/entitlement-version`

Decision input must include tenant, principal, action, resource type/ID, environment
attributes, and authentication context. The caller must not be able to assert a
different principal without an authorized delegation scope.

### Governance

- CRUD `/api/v1/access-requests`
- `POST /api/v1/access-requests/{requestId}/approve`
- `POST /api/v1/access-requests/{requestId}/deny`
- CRUD `/api/v1/access-reviews`
- `GET /api/v1/access-reviews/{campaignId}/items`
- `POST /api/v1/access-review-items/{itemId}/certify`
- `POST /api/v1/access-review-items/{itemId}/revoke`
- CRUD `/api/v1/separation-of-duties-rules`
- `POST /api/v1/break-glass-requests`
- `POST /api/v1/break-glass-requests/{requestId}/approve`

## 5.6 Notification APIs

- CRUD `/api/v1/notification-templates`
- `POST /api/v1/notification-templates/{templateId}/versions`
- `POST /api/v1/notification-templates/{templateId}/preview`
- `POST /api/v1/notification-templates/{templateId}/activate`
- `GET|PATCH /api/v1/notification-preferences/me`
- `POST /api/v1/notifications`
- `GET /api/v1/notifications/{notificationId}`
- `POST /api/v1/notifications/{notificationId}/retry`
- CRUD `/api/v1/webhook-subscriptions`
- `POST /api/v1/webhook-subscriptions/{subscriptionId}/rotate-secret`
- `POST /api/v1/webhook-subscriptions/{subscriptionId}/test`

Delivery log search must be an administrator API with tenant scoping, pagination, and
PII masking.

## 5.7 Audit APIs

- `GET /api/v1/audit/events`
- `GET /api/v1/audit/events/{eventId}`
- `POST /api/v1/audit/exports`
- `GET /api/v1/audit/exports/{exportId}`
- `GET /api/v1/audit/exports/{exportId}/download`
- CRUD `/api/v1/audit/retention-policies`
- CRUD `/api/v1/audit/legal-holds`
- CRUD `/api/v1/audit/siem-destinations`
- `POST /api/v1/audit/siem-destinations/{destinationId}/test`

Users should have a narrower endpoint:

- `GET /api/v1/users/me/security-events`

## 6. Missing Security Controls

## 6.1 Immediate Critical Controls

1. Rotate committed RSA keys and remove them from Git history.
2. Store signing keys in KMS/HSM/Vault and expose public keys through JWKS.
3. Add active Spring Security resource-server configuration to every business service.
4. Strip all client-supplied identity headers at the gateway.
5. Stop using caller-supplied `X-User-Id`; use the verified token principal.
6. Protect every internal endpoint with workload identity and service scopes.
7. Remove public test endpoints and restrict Swagger/Actuator by environment and role.
8. Expose only gateway/ingress publicly.
9. Require authorization on role, permission, tenant, member, template, notification,
   and audit operations.
10. Add explicit organization membership/ownership checks to prevent IDOR.

## 6.2 Token and Protocol Controls

- Authorization code with PKCE; no implicit flow.
- Exact redirect URI matching.
- Sender-constrained tokens where required through mTLS or DPoP.
- Audience and issuer validation by every resource server.
- Access-token type validation and scope enforcement.
- `kid`-based signing key rotation.
- Refresh-token rotation and reuse detection.
- Short-lived authorization codes and one-time challenges.
- State and nonce validation.
- Client secret hashing and rotation.
- Separate clients and audiences per environment.
- No tokens or authorization codes in logs or URLs.

## 6.3 Authentication Controls

- Per-account, per-IP, per-device, and global rate limits.
- Credential stuffing and password spraying detection.
- Breached-password screening.
- WebAuthn/passkey support for privileged users.
- Step-up MFA for role changes, security policy changes, exports, client secret
  operations, and break-glass access.
- Secure recovery that cannot bypass enrolled MFA without enhanced verification.
- New authenticator, password, email, session, and recovery notifications.
- Constant-time comparison for secret material.
- Encryption for TOTP secrets and federation credentials.

## 6.4 Authorization and Tenant Controls

- Deny by default.
- Every query for tenant data includes tenant scope.
- Tenant scope comes from trusted token/session context, not request body alone.
- Platform administration is separate from tenant administration.
- Prevent self-granting privilege escalation.
- Enforce grantor authority over the target scope.
- Require dual approval for critical global roles and break-glass access.
- Enforce separation-of-duties conflicts.
- Version and invalidate authorization caches.
- Test horizontal and vertical privilege escalation continuously.

## 6.5 Service and Network Controls

- mTLS or workload identity for service-to-service calls.
- Network policies that prevent direct public access.
- Explicit gateway route allowlist; disable automatic discovery routes.
- TLS everywhere and certificate rotation.
- Feign connect/read timeouts, circuit breakers, and bounded retries.
- Request body, header, query length, and upload limits.
- Egress allowlists for email, webhook, IdP, and SIEM destinations.
- SSRF controls for configurable URLs and webhooks.
- Signed webhooks with timestamp and replay window.

## 6.6 Data and Operational Controls

- Encryption at rest and field-level encryption for high-risk attributes.
- Secret references instead of plaintext configuration.
- Database least-privilege users per service.
- Backup encryption and tested restoration.
- Immutable production artifacts, SBOM, image signing, and vulnerability scanning.
- SAST, dependency, IaC, secret, and container scanning in CI.
- Structured logs with PII and secret redaction.
- OpenTelemetry traces and correlation IDs.
- Security alerts for abnormal login, MFA reset, role escalation, audit delivery failure,
  key rotation failure, and cross-tenant denial spikes.

## 7. Missing Audit Events

Use stable dot-delimited event names and versioned schemas.

## 7.1 Authentication Events

- `auth.registration.started`
- `auth.registration.completed`
- `auth.registration.failed`
- `auth.email_verification.issued`
- `auth.email_verification.completed`
- `auth.email_verification.failed`
- `auth.login.succeeded`
- `auth.login.failed`
- `auth.login.blocked`
- `auth.account.locked`
- `auth.account.unlocked`
- `auth.logout.completed`
- `auth.logout_all.completed`
- `auth.password.change.succeeded`
- `auth.password.change.failed`
- `auth.password.reset.requested`
- `auth.password.reset.completed`
- `auth.password.reset.failed`
- `auth.step_up.requested`
- `auth.step_up.succeeded`
- `auth.step_up.failed`

## 7.2 Authenticator and Session Events

- `auth.authenticator.enrolled`
- `auth.authenticator.verified`
- `auth.authenticator.removed`
- `auth.authenticator.recovery_used`
- `auth.recovery_codes.generated`
- `auth.recovery_codes.regenerated`
- `auth.session.created`
- `auth.session.refreshed`
- `auth.session.revoked`
- `auth.session.reuse_detected`
- `auth.device.registered`
- `auth.device.trusted`
- `auth.device.untrusted`
- `auth.risk.challenge_triggered`

## 7.3 OAuth/OIDC and Federation Events

- `oauth.authorization.requested`
- `oauth.authorization.approved`
- `oauth.authorization.denied`
- `oauth.token.issued`
- `oauth.token.refresh_succeeded`
- `oauth.token.refresh_failed`
- `oauth.token.revoked`
- `oauth.token.introspection_performed`
- `oauth.consent.granted`
- `oauth.consent.revoked`
- `oauth.client.created`
- `oauth.client.updated`
- `oauth.client.secret_created`
- `oauth.client.secret_rotated`
- `oauth.client.deleted`
- `oauth.signing_key.activated`
- `oauth.signing_key.retired`
- `federation.login.succeeded`
- `federation.login.failed`
- `federation.identity.linked`
- `federation.identity.unlinked`
- `federation.provider.created`
- `federation.provider.updated`
- `federation.provider.deleted`
- `federation.metadata_rotated`

## 7.4 User Lifecycle Events

- `user.created`
- `user.invited`
- `user.activated`
- `user.updated`
- `user.suspended`
- `user.unsuspended`
- `user.disabled`
- `user.deprovisioned`
- `user.restored`
- `user.deleted`
- `user.identifier.added`
- `user.identifier.verified`
- `user.identifier.removed`
- `user.profile.exported`
- `user.bulk_import.started`
- `user.bulk_import.completed`
- `user.bulk_import.failed`
- `user.privacy_request.created`
- `user.privacy_request.completed`

## 7.5 Organization Events

- `organization.created`
- `organization.updated`
- `organization.suspended`
- `organization.restored`
- `organization.deleted`
- `organization.domain.added`
- `organization.domain.verified`
- `organization.domain.removed`
- `organization.invitation.issued`
- `organization.invitation.resent`
- `organization.invitation.accepted`
- `organization.invitation.declined`
- `organization.invitation.revoked`
- `organization.invitation.expired`
- `organization.member.added`
- `organization.member.updated`
- `organization.member.removed`
- `organization.security_policy.updated`
- `organization.identity_provider.assigned`
- `organization.identity_provider.removed`
- `organization.unit.created`
- `organization.unit.updated`
- `organization.unit.deleted`

## 7.6 Authorization and Governance Events

- `authorization.role.created`
- `authorization.role.updated`
- `authorization.role.deleted`
- `authorization.permission.created`
- `authorization.permission.updated`
- `authorization.permission.deleted`
- `authorization.role.permission_added`
- `authorization.role.permission_removed`
- `authorization.group.created`
- `authorization.group.updated`
- `authorization.group.deleted`
- `authorization.group.member_added`
- `authorization.group.member_removed`
- `authorization.grant.created`
- `authorization.grant.updated`
- `authorization.grant.revoked`
- `authorization.decision.allowed`
- `authorization.decision.denied`
- `authorization.policy.created`
- `authorization.policy.updated`
- `authorization.policy.activated`
- `authorization.policy.deleted`
- `authorization.access_request.created`
- `authorization.access_request.approved`
- `authorization.access_request.denied`
- `authorization.access_review.started`
- `authorization.access_review.certified`
- `authorization.access_review.revoked`
- `authorization.access_review.completed`
- `authorization.sod.violation_detected`
- `authorization.break_glass.requested`
- `authorization.break_glass.approved`
- `authorization.break_glass.activated`
- `authorization.break_glass.expired`
- `authorization.break_glass.revoked`

Log all denied authorization decisions for sensitive operations. For ordinary high-volume
reads, use sampling or risk-based retention to control cost while preserving security
evidence.

## 7.7 Provisioning, Notification, and Audit Events

- `provisioning.scim.user.created`
- `provisioning.scim.user.updated`
- `provisioning.scim.user.deleted`
- `provisioning.scim.group.created`
- `provisioning.scim.group.updated`
- `provisioning.scim.group.deleted`
- `provisioning.job.started`
- `provisioning.job.completed`
- `provisioning.job.failed`
- `notification.requested`
- `notification.delivered`
- `notification.failed`
- `notification.suppressed`
- `notification.template.created`
- `notification.template.activated`
- `notification.preference.updated`
- `webhook.subscription.created`
- `webhook.subscription.updated`
- `webhook.subscription.deleted`
- `webhook.delivery.succeeded`
- `webhook.delivery.failed`
- `audit.export.requested`
- `audit.export.completed`
- `audit.export.downloaded`
- `audit.retention_policy.updated`
- `audit.legal_hold.created`
- `audit.legal_hold.released`
- `audit.siem_delivery.failed`
- `audit.integrity_check.failed`

## 8. Complete Implementation Plan

The phases below assume a focused platform team. Security-critical phases should not be
deferred in favor of adding more product modules.

## Phase 0 - Containment and Architecture Decisions (Week 1)

### Deliverables

- Rotate and remove committed signing keys.
- Block public access to internal services and operational endpoints.
- Remove or disable test controllers.
- Create architecture decision records for:
  - build versus adopt an external IdP;
  - tenant model;
  - authorization model;
  - service identity;
  - audit retention/integrity;
  - MySQL/RabbitMQ continuation.
- Publish a service ownership and data ownership map.

### Acceptance

- Secret scanning finds no active credential material.
- Only gateway is externally routable.
- Cross-tenant and anonymous organization access tests fail closed.

## Phase 1 - Engineering Foundation (Weeks 2-3)

### Deliverables

- Root Maven reactor and centralized dependency management.
- Flyway enabled for every stateful service.
- Testcontainers test profile for MySQL, Redis, and RabbitMQ.
- CI with unit/integration/security tests, SAST, dependency scanning, secret scanning,
  SBOM, image scanning, and signed artifacts.
- Standard Problem Details, pagination, correlation ID, and event envelope libraries.
- Outbox/inbox implementation template.

### Acceptance

- One command builds and tests all modules.
- No service depends on external Docker configuration merely to load its unit tests.
- Schema migrations can create every database from empty.

## Phase 2 - Security Enforcement Baseline (Weeks 3-5)

### Deliverables

- Replace custom ad hoc filters with Spring OAuth2 resource-server configuration.
- Issuer, audience, scope, token type, and key ID validation.
- Active security configuration in organization and notification services.
- Service-to-service authentication and internal scopes.
- Gateway route allowlist, header stripping, rate limits, and security headers.
- Tenant context and resource authorization utilities.
- Administrator protection on all mutation endpoints.

### Acceptance

- Automated tests cover anonymous, wrong-tenant, wrong-role, expired-token, wrong-audience,
  and forged-header scenarios.
- Direct service access cannot bypass gateway policy.

## Phase 3 - OAuth/OIDC Authorization Server and Sessions (Weeks 5-9)

### Deliverables

- OpenID discovery and JWKS.
- Authorization code with PKCE.
- Client credentials.
- ID tokens, user-info, revocation, introspection, consent, and logout.
- Client administration and secret rotation.
- Refresh-token families with reuse detection.
- User session/device APIs and revocation.
- KMS/HSM-backed signing and overlapping key rotation.

### Acceptance

- Conformance-oriented protocol tests pass.
- Key rotation causes no token-validation outage.
- Reused refresh token revokes the full token family and creates a high-severity event.

## Phase 4 - User Lifecycle, Recovery, and MFA (Weeks 8-12)

### Deliverables

- Email verification and password reset.
- Password change/history/breached-password controls.
- TOTP and WebAuthn/passkeys.
- Recovery codes and secure authenticator recovery.
- Administrative user lifecycle APIs.
- Session revocation tied to user and credential state changes.
- Security notifications and user-visible security history.

### Acceptance

- Privileged actions require step-up authentication.
- Suspended/deprovisioned users cannot refresh or create sessions.
- Recovery cannot silently remove MFA without audit and notification.

## Phase 5 - Tenant Model and Organization Lifecycle (Weeks 10-14)

### Deliverables

- Tenant ID propagated through tokens, requests, tables, events, and audit.
- Invitations and membership lifecycle.
- Domain verification.
- Organization units/hierarchy.
- Tenant security policy and branding.
- Tenant suspension, export, and deletion workflows.
- Tenant-specific IdP and client association.

### Acceptance

- Dedicated cross-tenant isolation suite passes for every service.
- All tenant table indexes begin with tenant scope where appropriate.
- Organization administrators cannot administer another tenant.

## Phase 6 - Authorization Platform (Weeks 12-17)

### Deliverables

- Remove duplicate role authority from authentication-service.
- Tenant/platform/application roles.
- Groups, role grants, permission grants, scoped/time-bound access.
- Batch authorization and explain APIs.
- Policy sets and conditions.
- Entitlement versioning and cache invalidation.
- Delegated administration and separation-of-duties.

### Acceptance

- Role/permission changes take effect within the documented revocation objective.
- Self-escalation, transitive group cycles, and conflicting grants are blocked.
- Every sensitive API has explicit policy tests.

## Phase 7 - Central Audit Service (Weeks 14-18)

### Deliverables

- Canonical audit event SDK and schema registry.
- Append-only audit ingestion from RabbitMQ.
- Outbox-based publication from every service.
- Search, export, retention, legal hold, SIEM, and archive APIs.
- Tamper-evident chain or signed immutable archive batches.
- Dashboards and alerts for missing event streams and integrity failures.

### Acceptance

- Every event family in Section 7 has producer, schema, retention, and test ownership.
- A business transaction and its audit event cannot silently diverge.
- Audit exports are access-controlled, checksummed, time-limited, and themselves audited.

## Phase 8 - Federation and SCIM (Weeks 17-22)

### Deliverables

- Tenant OIDC and SAML federation.
- Home realm discovery and JIT provisioning.
- Secure account linking.
- SCIM Users, Groups, schemas, filtering, patch, bulk, and ETags.
- Provisioning jobs, reconciliation, and deprovisioning.

### Acceptance

- Federation metadata/certificate rotation is tested.
- SCIM deactivation revokes sessions and access.
- Provisioning retries are idempotent and observable.

## Phase 9 - IAM Governance (Weeks 21-26)

### Deliverables

- Access requests and approval workflows.
- Time-bound privileged access.
- Access review campaigns.
- Separation-of-duties reporting.
- Break-glass access.
- Orphaned, inactive, and excessive-access reports.

### Acceptance

- Privileged grants require policy-compliant approval and expiry.
- Review decisions create revocation work and immutable evidence.
- Break-glass use triggers immediate security notification.

## Phase 10 - Production Hardening and Scale (Weeks 24-28)

### Deliverables

- Kubernetes/Helm, external secrets, network policies, probes, limits, HPA, and PDB.
- OpenTelemetry, Prometheus, dashboards, SLOs, and paging alerts.
- Feign timeouts, circuit breakers, bulkheads, bounded retries, and load shedding.
- Backup, restore, regional recovery, key compromise, and account takeover runbooks.
- Performance, soak, chaos, penetration, and disaster recovery tests.

### Acceptance

- Defined authentication and authorization SLOs are met under target load.
- Recovery objectives are demonstrated in an exercise.
- No critical or high exploitable findings remain before production release.

## 9. Testing Strategy

### Per service

- Unit tests for policy and lifecycle rules.
- Repository tests against the real database engine.
- Controller tests for authentication, authorization, validation, and error contracts.
- Migration tests from every supported prior schema.
- Outbox/inbox and duplicate-delivery tests.
- Audit event schema and redaction tests.

### Platform tests

- OAuth/OIDC protocol and negative tests.
- Tenant-isolation matrix.
- IDOR and privilege-escalation suite.
- Refresh-token replay and session-revocation suite.
- MFA enrollment, removal, recovery, and step-up suite.
- SCIM compatibility and idempotency suite.
- Contract tests between every synchronous client/provider pair.
- Event compatibility tests for every producer/consumer pair.
- Load tests for login, token refresh, authorization checks, and audit ingestion.
- Fault tests for Redis, RabbitMQ, database, IdP, email, and SIEM outages.

## 10. Delivery Priorities

### P0 - Complete before any external production use

- Key rotation and secret removal.
- Organization and internal endpoint protection.
- Tenant/resource authorization.
- Centralized dependency/build/test baseline.
- Database migrations.
- Secure token validation and session revocation.
- Central audit ingestion for security and admin events.

### P1 - Enterprise pilot

- Standards-compliant OAuth/OIDC.
- MFA/passkeys and recovery.
- User lifecycle and organization invitations.
- Tenant-scoped RBAC/groups.
- Session/device management.
- SIEM integration and operational observability.

### P2 - Broad enterprise adoption

- OIDC/SAML federation.
- SCIM.
- Delegated administration.
- Access requests and reviews.
- Separation-of-duties and break-glass.
- Advanced risk-based authentication.

## 11. Definition of Enterprise Ready

The IAM platform is enterprise ready only when:

- Credentials and keys are managed outside source and images.
- OAuth/OIDC protocol behavior is standards compliant.
- Every service validates trusted identity and enforces tenant/resource authorization.
- User, credential, session, membership, and grant lifecycle changes revoke access within
  a documented objective.
- All schema changes are migrated and recoverable.
- Security-sensitive actions produce complete immutable audit evidence.
- Federation and SCIM lifecycle behavior are idempotent and observable.
- CI, observability, incident response, backup restore, key rotation, and disaster
  recovery are operational and tested.
- Cross-tenant, IDOR, privilege-escalation, and token-replay test suites pass.

## 12. Standards Baseline

Implementation should align with:

- OAuth 2.0 Security Best Current Practice, RFC 9700.
- OpenID Connect Core 1.0.
- SCIM Core Schema and Protocol, RFC 7643 and RFC 7644.
- NIST SP 800-63B authentication and authenticator lifecycle guidance.
- WebAuthn for phishing-resistant authenticators.

