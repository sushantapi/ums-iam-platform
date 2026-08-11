# Internal Contract Inventory

Status: Complete for Task 10

All routes in this inventory require `X-Internal-Service-Secret`. They do not depend on bearer JWTs, gateway identity headers, or `X-Internal-Gateway-Secret`.

| Provider service | Method | Route | Purpose | Caller service(s) |
|---|---|---|---|---|
| user-service | GET | `/api/v1/internal/users` | Bounded, paginated user summaries | admin-service |
| user-service | GET | `/api/v1/internal/users/{userId}` | User lookup | organization-service |
| authorization-service | GET | `/api/v1/internal/users/{userId}/authorization` | Roles and permissions snapshot | authentication-service |
| authorization-service | POST | `/api/v1/internal/users/{userId}/roles/default` | Assign registration default role | authentication-service |
| authorization-service | POST | `/api/v1/internal/roles/assign` | Administrative role assignment | admin-service |
| audit-service | GET | `/api/v1/internal/audit/events` | Bounded audit query for the admin BFF | admin-service |

## Feign Verification

| Caller | Client | Result |
|---|---|---|
| admin-service | user-service internal list | Uses canonical internal route and shared internal-secret interceptor |
| admin-service | audit-service internal query | Uses canonical internal route and shared internal-secret interceptor |
| admin-service | authorization-service role assignment | Uses canonical internal route and shared internal-secret interceptor |
| organization-service | user-service lookup | Uses canonical internal route and internal-secret interceptor |
| authentication-service | authorization snapshot/default role | Uses canonical internal routes and internal-secret interceptor |

No Feign client forwards bearer tokens, gateway secrets, or `X-Authenticated-User`.

Notification-service and admin-service currently define internal security namespaces but expose no production internal controller routes. Boundary tests use a nonexistent internal path to prove that the internal-secret chain is isolated from external gateway trust.

The stale authorization-service self-client and legacy `/api/v1/authorization/internal/**` route were removed.
