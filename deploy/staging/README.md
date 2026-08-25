# UMS IAM + HRMS low-memory staging

This staging profile is intended for low-traffic demo/beta validation on an ARM64 or AMD64 Linux host with about 12 GB RAM. It reuses the production topology and security boundary, but applies tighter memory limits.

It is not the recommended sizing for real customer production workloads.

## Multi-architecture images

The service image workflow publishes every backend image for both:

- `linux/amd64`
- `linux/arm64`

The Java build stage runs on the CI runner's native build platform. Because Spring Boot JAR/WAR artifacts are architecture-neutral, Buildx can reuse that build output while selecting the correct JRE runtime image for each target architecture.

Do not pin a platform in Compose. Docker should select the host-compatible image from the multi-architecture manifest automatically.

## Staging Compose overlay

`docker-compose.staging.yml` must be used together with `docker-compose.prod.yml`; it is not standalone.

The staging hard limits total about 9.6 GB across Caddy, MySQL, RabbitMQ, Redis and the 14 JVM services. This leaves host memory for Linux, Docker, page cache and startup/native memory.

Key staging limits:

- Caddy: 128 MB
- MySQL: 1536 MB, 512 MB InnoDB buffer pool, max 80 connections
- RabbitMQ: 768 MB
- Redis: 256 MB
- Discovery + Config: 384 MB container / 192 MB Java heap each
- Remaining JVM services: 512 MB container / 256 MB Java heap each

## Validate locally

From the repository root on a workstation with Docker Compose v2 and PowerShell:

```powershell
./scripts/validate-staging-compose.ps1
```

Expected terminal marker:

```text
STAGING_COMPOSE_VALIDATION=PASS
```

The validator checks that:

- production + staging Compose render together
- only Caddy publishes host ports
- application services use GHCR images and production Spring profiles
- no application service pins an architecture
- memory limits match the staging budget
- MySQL low-memory tuning is present
- the Dockerfile compiles on `BUILDPLATFORM`
- the publish workflow targets and verifies AMD64 + ARM64 manifests

## Deploy staging

Keep the real staging environment file outside Git. Then run the production Compose file plus the staging overlay:

```bash
docker compose \
  --env-file /path/to/staging.env \
  -f docker-compose.prod.yml \
  -f docker-compose.staging.yml \
  config

docker compose \
  --env-file /path/to/staging.env \
  -f docker-compose.prod.yml \
  -f docker-compose.staging.yml \
  pull

docker compose \
  --env-file /path/to/staging.env \
  -f docker-compose.prod.yml \
  -f docker-compose.staging.yml \
  up -d
```

Use the existing production environment template and deployment runbook for required configuration, JWT keys, SMTP, backup, rollback and smoke-test rules.

## Acceptance on the host

Before using the staging environment for demos:

1. all expected containers become healthy
2. Caddy is the only public service and HTTPS routes to API Gateway
3. internal database/cache/broker/service ports are not publicly reachable
4. login and dashboard work
5. IAM and HRMS read paths work
6. logout invalidates the old access token
7. basic notification delivery works when staging SMTP is configured
8. persistent data survives application container recreation

If the 12 GB host is unstable under the real workload, do not weaken security or remove required services. Move to a larger paid host instead.
