# UMS IAM + HRMS V1 production baseline

This directory defines the initial production deployment model for UMS IAM + HRMS. It deliberately targets a **single Linux VM + Docker Compose** for the first beta release. Kubernetes is not required for this slice.

## Topology

```text
Internet
  -> Caddy :80/:443
      -> API Gateway :8080 (Docker network only)
          -> IAM + HRMS services (Docker network only)
              -> MySQL / RabbitMQ / Redis (Docker network only)
```

Only Caddy publishes host ports. Eureka, Config Server, API Gateway, MySQL, RabbitMQ management, Redis and individual application services stay private to the Compose network.

The Admin Portal is a static Vite application and should be hosted separately. Build it with `VITE_API_BASE_URL=https://<api-domain>` and `VITE_USE_MOCKS=false`.

## Prerequisites

- Linux VM with Docker Engine and Docker Compose v2
- DNS `A/AAAA` record for the API hostname pointing at the VM
- inbound firewall rules for TCP 80 and 443 only (plus SSH restricted to trusted source addresses)
- access to the GHCR packages for this repository
- an approved version whose 14 service images were published by `.github/workflows/publish-images.yml`

A 4 vCPU / 16 GB RAM VM is a reasonable starting point for a low-traffic beta. Tune JVM limits from measured usage rather than assuming this is a permanent production size.

## 1. Prepare the host checkout

Checkout the exact release source so the production Compose file and Config Server repository match the application images:

```bash
git fetch --tags
git checkout <release-tag-or-exact-main-sha>
```

Do not run production from a dirty checkout.

## 2. Create production environment file

```bash
cp deploy/production/.env.example deploy/production/.env
chmod 600 deploy/production/.env
```

Fill every blank secret/value. Never copy local development credentials into this file.

Required secret material includes:

- MySQL root/application passwords
- RabbitMQ credentials
- Redis password
- `INTERNAL_GATEWAY_SECRET`
- `INTERNAL_SERVICE_SECRET`
- `MFA_ENCRYPTION_KEY`
- SMTP credentials
- a fresh JWT signing key pair

Generate high-entropy values with a password manager or `openssl rand`. Keep the real `.env` and key material off Git.

## 3. Generate production JWT keys

From the repository root on the production host:

```bash
mkdir -p secrets/jwt
chmod 700 secrets secrets/jwt

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out secrets/jwt/private_key.pem
openssl rsa -pubout -in secrets/jwt/private_key.pem -out secrets/jwt/public_key.pem

chmod 600 secrets/jwt/private_key.pem
chmod 644 secrets/jwt/public_key.pem
```

Set a new `JWT_KEY_ID` in `deploy/production/.env`. Never reuse the local/demo private key.

## 4. Validate before deployment

Login to GHCR if the package is private:

```bash
echo "$GHCR_TOKEN" | docker login ghcr.io -u <github-user> --password-stdin
```

Then render the production Compose model:

```bash
docker compose \
  --env-file deploy/production/.env \
  -f docker-compose.prod.yml \
  config >/tmp/ums-prod-compose.yml
```

The command must succeed before any deployment. The rendered model must publish only Caddy ports 80/443.

On a Windows development workstation, `scripts/validate-production-compose.ps1` performs the same structural gate with safe dummy values.

## 5. Deploy / update

```bash
docker compose --env-file deploy/production/.env -f docker-compose.prod.yml pull

docker compose --env-file deploy/production/.env -f docker-compose.prod.yml up -d

docker compose --env-file deploy/production/.env -f docker-compose.prod.yml ps
```

Flyway migrations run as each service starts. Migrations are forward-only: do not plan application rollback around reverse Flyway migrations.

Inspect a failing service with:

```bash
docker compose --env-file deploy/production/.env -f docker-compose.prod.yml logs --tail=200 <service>
```

## 6. Smoke gate

After all service health checks are `healthy`:

```bash
curl -fsS "https://${API_DOMAIN}/actuator/health" || true
```

The public proxy intentionally blocks `/actuator*`, so a 404 is acceptable and confirms that actuator is not exposed. Perform the product smoke through supported API/UI paths instead:

1. Admin login
2. Dashboard load
3. HRMS employee/read path
4. Logout
5. Reuse old access token -> `401`

Also confirm from outside the VM that ports such as 3306, 5672, 6379, 8080, 8761 and 8888 are not reachable.

## 7. Frontend production build

Use `frontend/admin-portal/.env.production.example` as the template:

```bash
cd frontend/admin-portal
VITE_API_BASE_URL="https://api.example.com" VITE_USE_MOCKS=false npm run build
```

Deploy `dist/` to static hosting with SPA fallback to `index.html`. The frontend origin must be allowed by the production gateway/CORS configuration for the chosen domain.

## 8. Backups

For the single-VM beta, take an encrypted off-host database backup at least daily. A baseline command is provided by `scripts/production/backup-mysql.sh`.

Example:

```bash
BACKUP_DIR=/srv/ums-backups/mysql \
BACKUP_RETENTION_DAYS=14 \
scripts/production/backup-mysql.sh
```

Copy completed `.sql.gz` and `.sha256` files to a separate encrypted storage location. A backup that exists only on the application VM is not an adequate disaster-recovery backup.

Organization assets live in the `organization_assets` Docker volume. Back it up separately, for example:

```bash
docker run --rm \
  -v ums-iam-prod_organization_assets:/source:ro \
  -v /srv/ums-backups/assets:/backup \
  alpine:3.20 \
  sh -c 'cd /source && tar -czf /backup/organization-assets-$(date +%Y%m%d-%H%M%S).tar.gz .'
```

For real customer IAM/employee/payroll data, prefer a managed MySQL service with provider backups/PITR instead of treating the local Compose database as the long-term production database.

## 9. Restore test

Restore drills must be performed before real customer data is accepted.

`restore-mysql.sh` requires an explicit confirmation flag:

```bash
CONFIRM_RESTORE=YES scripts/production/restore-mysql.sh /path/to/backup.sql.gz
```

Run restores in a maintenance/test environment first. Verify Flyway history, login, tenant data and a representative HRMS/payroll read after restore.

## 10. Application rollback

Application images are published with both version and exact source SHA tags. Production uses the version tag from `APP_VERSION`.

To roll application code back:

1. choose the previous known-good version
2. verify its GHCR images still exist
3. set `APP_VERSION=<previous-version>` in the protected production env file
4. `docker compose ... pull`
5. `docker compose ... up -d`
6. repeat the health and product smoke gates

Do **not** assume database schema can be rolled backward. If the newer release ran a destructive/incompatible migration, use a tested forward fix or restore strategy.

## Release order

The intended order is:

```text
main exact SHA green
  -> Publish Service Images workflow
  -> verify version + sha-* GHCR tags
  -> GitHub Release workflow
  -> production pull/up
  -> health + product smoke
```

The Release workflow refuses to create a release when the exact-main-SHA image set is missing.
