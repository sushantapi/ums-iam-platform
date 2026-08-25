#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: CONFIRM_RESTORE=YES $0 /path/to/backup.sql.gz" >&2
  exit 2
fi

if [[ "${CONFIRM_RESTORE:-}" != "YES" ]]; then
  echo "ERROR: restore is destructive. Re-run with CONFIRM_RESTORE=YES after verifying the target environment." >&2
  exit 2
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-${REPO_ROOT}/docker-compose.prod.yml}"
ENV_FILE="${ENV_FILE:-${REPO_ROOT}/deploy/production/.env}"
BACKUP_FILE="$1"
CHECKSUM_FILE="${BACKUP_FILE}.sha256"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: production env file not found: $ENV_FILE" >&2
  exit 1
fi

if [[ ! -f "$BACKUP_FILE" ]]; then
  echo "ERROR: backup file not found: $BACKUP_FILE" >&2
  exit 1
fi

if [[ -f "$CHECKSUM_FILE" ]]; then
  echo "Verifying checksum..."
  (cd "$(dirname "$BACKUP_FILE")" && sha256sum -c "$(basename "$CHECKSUM_FILE")")
else
  echo "WARNING: checksum file not found: $CHECKSUM_FILE" >&2
fi

compose=(docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE")

if ! "${compose[@]}" ps --status running mysql >/dev/null 2>&1; then
  echo "ERROR: mysql service is not running." >&2
  exit 1
fi

cat <<'WARN'
WARNING: restoring an all-databases dump will overwrite data in the target MySQL instance.
Application services should be stopped or the environment should be isolated before continuing.
WARN

read -r -p "Type RESTORE to continue: " confirmation
if [[ "$confirmation" != "RESTORE" ]]; then
  echo "Restore cancelled."
  exit 2
fi

echo "Restoring $BACKUP_FILE ..."
gzip -dc "$BACKUP_FILE" \
  | "${compose[@]}" exec -T mysql sh -c \
      'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot'

echo "Restore complete."
echo "Next: restart application services, verify Flyway history, then run login/tenant/HRMS smoke checks."
