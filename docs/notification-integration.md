# Notification Service Integration

Notification delivery is event-driven. Frontend applications do not call `notification-service`.

## Live Event Flows

| Producer | Event | Exchange / routing key | Queue |
| --- | --- | --- | --- |
| `authentication-service` | `UserRegisteredEvent` | `user.exchange` / `user.registered` | `notification.user.registered.queue` |
| `organization-service` | `OrganizationCreatedEvent` | `organization.exchange` / `organization.created` | `notification.organization.created.queue` |
| `authorization-service` | `RoleAssignedEvent` | `auth.exchange` / `role.assigned` | `role.assigned.queue` |

## Ready Auth Flows

The shared contracts, bindings, queues, and consumers are ready for:

- `EmailVerificationEvent` via `auth.email.verification.requested`
- `PasswordResetEvent` via `auth.password.reset.requested`
- `MfaOtpEvent` via `auth.mfa.otp.requested`

The authentication endpoints that create these events are still pending.

## Delivery Lifecycle

1. A Rabbit consumer receives a shared `common-events` DTO.
2. `notification-service` persists a `PENDING` row in `notification_events`.
3. The template is rendered and SMTP delivery is attempted.
4. Success marks the event `SENT` and writes a successful `notification_logs` row.
5. Failure marks the event `FAILED`, writes a failed log, and acknowledges the Rabbit message.
6. The scheduler retries failed events up to three times from their persisted payload.

## Mail Configuration

No SMTP credentials are committed. Configure:

```text
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
MAIL_SMTP_AUTH
MAIL_STARTTLS_ENABLE
MAIL_STARTTLS_REQUIRED
```

Without credentials, the service still starts and consumes events. Delivery failures are persisted for retry.

## Local Verification

- Service: `http://localhost:8085/actuator/health`
- Eureka: `NOTIFICATION-SERVICE` should be `UP`
- RabbitMQ queues should each have one consumer
- Database: `notification_db.notification_events` and `notification_logs`
