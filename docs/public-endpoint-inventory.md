# Public Endpoint Inventory

Status: Complete for Task 10

Hard rule: any anonymously accessible application route not listed here is a Phase 0 lockdown defect.

| Service | Method | Route | Why public | Expected caller |
|---|---|---|---|---|
| authentication-service | POST | `/api/v1/auth/register` | Account onboarding | Public client |
| authentication-service | POST | `/api/v1/auth/login` | Credential authentication | Public client |
| authentication-service | POST | `/api/v1/auth/refresh` | Refresh-token exchange | Public client |
| api-gateway | GET | `/actuator/health` | Gateway health probe | Infrastructure |
| api-gateway | GET | `/actuator/info` | Basic gateway metadata | Infrastructure |
| authentication-service | GET | `/actuator/health` | Service health probe | Infrastructure |
| authentication-service | GET | `/actuator/info` | Basic service metadata | Infrastructure |
| user-service | GET | `/actuator/health` | Service health probe | Infrastructure |
| user-service | GET | `/actuator/info` | Basic service metadata | Infrastructure |
| authorization-service | GET | `/actuator/health` | Service health probe | Infrastructure |
| authorization-service | GET | `/actuator/info` | Basic service metadata | Infrastructure |
| organization-service | GET | `/actuator/health` | Service health probe | Infrastructure |
| organization-service | GET | `/actuator/info` | Basic service metadata | Infrastructure |
| notification-service | GET | `/actuator/health` | Service health probe | Infrastructure |
| notification-service | GET | `/actuator/info` | Basic service metadata | Infrastructure |
| audit-service | GET | `/actuator/health` | Service health probe | Infrastructure |
| audit-service | GET | `/actuator/info` | Basic service metadata | Infrastructure |
| admin-service | GET | `/actuator/health` | Service health probe | Infrastructure |
| admin-service | GET | `/actuator/info` | Basic service metadata | Infrastructure |

## Explicitly Non-Public Patterns

- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/swagger-resources/**`
- `/test/**`
- `/api/test/**`
- `/h2-console/**`
- `/api/v1/internal/**`
- `/internal/**`
- `/actuator/**` other than health and info

## Infrastructure Control Plane

Eureka and Spring Cloud Config are infrastructure endpoints, not public application APIs. They are not routed by the API gateway. The current local Docker composition still publishes their host ports, so production deployment must place them on a private management network or add control-plane authentication.

The discovery test route `/test/discovery` is now dev-profile only.

## Verification

- Gateway anonymous auth allow-list is restricted to the three implemented POST routes above.
- Removed nonexistent forgot-password, reset-password, and email-verification routes from the gateway public allow-list.
- Gateway discovery locator is disabled; only explicit route definitions are exposed.
- Swagger is disabled in user-service, organization-service, and authorization-service and remains non-public in the other hardened services.
- Docker actuator exposure is reduced to health and info.
