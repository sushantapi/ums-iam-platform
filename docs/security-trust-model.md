# Security Trust Model

## External Request Flow

Clients send JWTs to the API Gateway. The gateway validates the token, strips spoofable identity headers, and injects trusted identity headers plus `X-Internal-Gateway-Secret`.

Downstream business services trust gateway identity only when the gateway secret matches. Direct bearer JWTs to downstream services are not a substitute for trusted gateway headers.

## Internal Service Flow

Internal service endpoints live under `/api/v1/internal/**` where applicable. Internal callers authenticate with `X-Internal-Service-Secret`.

Internal routes do not rely on gateway identity headers, and gateway-mediated routes do not rely on the internal service secret.

## Token Ownership

Only `authentication-service` signs JWTs. Other services validate public keys only when required by their local runtime wiring or specialized routes.

JWT private keys are never committed. They must come from a runtime secret source such as a mounted secret file or secret-manager-provided environment value.

## Public Key Distribution

Gateway and JWT-validating services consume public key material from a mounted secret path or an environment-provided PEM value.

Local Docker uses `./secrets/jwt` mounted read-only into services as `/run/secrets/jwt`.

## Secret Handling

`INTERNAL_GATEWAY_SECRET` identifies trusted gateway-originated traffic to downstream services.

`INTERNAL_SERVICE_SECRET` identifies service-to-service calls to internal endpoints.

UAT and production must source these values from environment-specific secret storage, not from committed config defaults.
