#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-${REPO_ROOT}/docker-compose.prod.yml}"
ENV_FILE="${ENV_FILE:-${REPO_ROOT}/deploy/production/.env}"
BACKUP_DIR="${BACKUP_DIR:-${REPO_ROOT}/backups/mysql}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: production env file not found: $ENV_FILE" >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR" || true

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup_file="${BACKUP_DIR}/ums-mysql-${timestamp}.sql.gz"
checksum_file="${backup_file}.sha256"

compose=(docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE")

if ! "${compose[@]}" ps --status running mysql >/dev/null 2>&1; then
  echo "ERROR: mysql service is not running." >&2
  exit 1
fi

echo "Creating consistent MySQL backup: $backup_file"
"${compose[@]}" exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --all-databases --single-transaction --quick --routines --events --triggers --hex-blob' \
  | gzip -9 > "$backup_file"

if [[ ! -s "$backup_file" ]]; then
  echo "ERROR: backup file is empty." >&2
  rm -f "$backup_file"
  exit 1
fi

sha256sum "$backup_file" > "$checksum_file"

find "$BACKUP_DIR" -type f \
  \( -name 'ums-mysql-*.sql.gz' -o -name 'ums-mysql-*.sql.gz.sha256' \) \
  -mtime "+${BACKUP_RETENTION_DAYS}" -delete

echo "Backup complete."
echo "  file: $backup_file"
echo "  checksum: $checksum_file"
echo "Copy both files to encrypted off-host storage."
