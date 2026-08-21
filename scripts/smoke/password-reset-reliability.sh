#!/usr/bin/env bash
set -Eeuo pipefail

# Runtime acceptance for Issue #27.
# This script intentionally stops Mailpit to force password-reset delivery failure.
# It must only be run against an isolated local/dev Docker stack.
#
# Required:
#   ALLOW_RUNTIME_FAILURE_INJECTION=1
#   SMOKE_EMAIL=<existing local smoke user email>
#
# Optional:
#   PROJECT=<docker compose project>
#   ENV_FILE=<compose env file path>
#   COMPOSE_FILE=<compose file path>
#   EXPECTED_ATTEMPTS=3
#   REBUILD_NOTIFICATION=1

PROJECT="${PROJECT:-ums-iam-platform-feature-admin-portal-integration}"
ENV_FILE="${ENV_FILE:-.env}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.yml}"
EXPECTED_ATTEMPTS="${EXPECTED_ATTEMPTS:-3}"
REBUILD_NOTIFICATION="${REBUILD_NOTIFICATION:-1}"

MAILPIT_CONTAINER="ums-mailpit"
NOTIFICATION_CONTAINER="ums-notification-service"
RABBIT_CONTAINER="ums-rabbitmq"
MYSQL_CONTAINER="ums-mysql"
PASSWORD_RESET_QUEUE="notification.password.reset.queue"
PASSWORD_RESET_DLQ="notification.password.reset.dlq"

TMP_DIR="$(mktemp -d 2>/dev/null || mktemp -d -t ums-password-reset-reliability)"
MAILPIT_WAS_STOPPED=0

cleanup() {
  local rc=$?
  if [ "$MAILPIT_WAS_STOPPED" = "1" ]; then
    docker start "$MAILPIT_CONTAINER" >/dev/null 2>&1 || true
  fi
  rm -rf "$TMP_DIR"
  exit "$rc"
}
trap cleanup EXIT

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "required command not found: $1"
}

http_code() {
  local url="$1"
  curl --noproxy '*' --max-time 10 -sS -o /dev/null -w '%{http_code}' "$url" 2>/dev/null || true
}

queue_metric() {
  local queue="$1"
  local metric="$2"
  docker exec "$RABBIT_CONTAINER" rabbitmqctl list_queues name "$metric" --formatter=tsv 2>/dev/null \
    | tr -d '\r' \
    | awk -v q="$queue" '$1 == q {print $2; exit}'
}

mailpit_count() {
  local output_file="$1"
  curl --noproxy '*' --max-time 10 -sS \
    http://127.0.0.1:8025/api/v1/messages >"$output_file"

  export MAILPIT_INDEX_WIN
  MAILPIT_INDEX_WIN="$(cygpath -w "$output_file")"
  export SMOKE_EMAIL

  powershell.exe -NoProfile -Command '
    $data = Get-Content -LiteralPath $env:MAILPIT_INDEX_WIN -Raw | ConvertFrom-Json
    $email = $env:SMOKE_EMAIL.ToLowerInvariant()
    $count = 0
    foreach ($message in @($data.messages)) {
      if ([string]$message.Subject -ne "Reset your UMS password") { continue }
      foreach ($recipient in @($message.To)) {
        $address = if ($null -ne $recipient.Address) { [string]$recipient.Address } else { [string]$recipient }
        if ($address.ToLowerInvariant() -eq $email) { $count++; break }
      }
    }
    Write-Output $count
  ' | tr -d '\r'
}

require_command docker
require_command curl
require_command awk
require_command grep
require_command sed
require_command cygpath
require_command powershell.exe

[ "${ALLOW_RUNTIME_FAILURE_INJECTION:-0}" = "1" ] \
  || fail "set ALLOW_RUNTIME_FAILURE_INJECTION=1 to acknowledge local failure injection"

[ -n "${SMOKE_EMAIL:-}" ] || fail "SMOKE_EMAIL is required"
[[ "$SMOKE_EMAIL" =~ ^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+$ ]] \
  || fail "SMOKE_EMAIL contains unsupported characters"
[[ "$EXPECTED_ATTEMPTS" =~ ^[1-9][0-9]*$ ]] || fail "EXPECTED_ATTEMPTS must be a positive integer"
[ -f "$COMPOSE_FILE" ] || fail "compose file not found: $COMPOSE_FILE"
[ -f "$ENV_FILE" ] || fail "env file not found: $ENV_FILE"

docker info >/dev/null 2>&1 || fail "Docker engine is not ready"

echo "=== PASSWORD RESET RELIABILITY RUNTIME GATE ==="
echo "CURRENT_HEAD=$(git rev-parse HEAD 2>/dev/null || echo unknown)"

if [ "$REBUILD_NOTIFICATION" = "1" ]; then
  echo "Rebuilding notification-service from current checkout..."
  docker compose -p "$PROJECT" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" \
    build notification-service
  docker compose -p "$PROJECT" --env-file "$ENV_FILE" -f "$COMPOSE_FILE" \
    up -d --no-deps --force-recreate notification-service
fi

for i in {1..60}; do
  if [ "$(http_code http://127.0.0.1:8085/actuator/health)" = "200" ]; then
    break
  fi
  [ "$i" -lt 60 ] || fail "notification-service did not become healthy"
  sleep 3
done

for gate in \
  "GATEWAY|http://127.0.0.1:8080/actuator/health" \
  "AUTH|http://127.0.0.1:8086/actuator/health" \
  "NOTIFICATION|http://127.0.0.1:8085/actuator/health" \
  "MAILPIT|http://127.0.0.1:8025/api/v1/messages"
do
  name="${gate%%|*}"
  url="${gate#*|}"
  code="$(http_code "$url")"
  echo "$name=$code"
  [ "$code" = "200" ] || fail "$name health gate failed"
done

USER_EXISTS=$(docker exec "$MYSQL_CONTAINER" sh -lc \
  "mysql -N -B -uroot -p\"\$MYSQL_ROOT_PASSWORD\" auth_db -e \"SELECT COUNT(*) FROM users WHERE email='$SMOKE_EMAIL';\"" \
  2>/dev/null | tr -d '\r')
[ "$USER_EXISTS" = "1" ] || fail "SMOKE_EMAIL must already exist in auth_db"
echo "SMOKE_USER=PASS"

DLQ_DECLARED=$(docker exec "$RABBIT_CONTAINER" rabbitmqctl list_queues name --formatter=tsv 2>/dev/null \
  | tr -d '\r' | awk -v q="$PASSWORD_RESET_DLQ" '$1 == q {print 1; exit}')
[ "$DLQ_DECLARED" = "1" ] || fail "password reset DLQ is not declared"
echo "PASSWORD_RESET_DLQ_DECLARED=PASS"

docker exec "$RABBIT_CONTAINER" rabbitmqctl purge_queue "$PASSWORD_RESET_DLQ" >/dev/null

BASELINE_MAIL_COUNT=$(mailpit_count "$TMP_DIR/mailpit-before.json")
echo "MAILPIT_BASELINE_RESET_COUNT=$BASELINE_MAIL_COUNT"

TEST_SINCE="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"

echo "Injecting SMTP failure by stopping Mailpit..."
docker stop "$MAILPIT_CONTAINER" >/dev/null
MAILPIT_WAS_STOPPED=1

FORGOT_HTTP=$(curl --noproxy '*' --max-time 20 -sS \
  -o "$TMP_DIR/forgot-failure.json" \
  -w '%{http_code}' \
  -X POST \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$SMOKE_EMAIL\"}" \
  http://127.0.0.1:8080/api/v1/auth/forgot-password || true)
echo "FAILURE_INJECTION_FORGOT_HTTP=$FORGOT_HTTP"
[ "$FORGOT_HTTP" = "200" ] || fail "forgot-password request failed during failure injection"

DLQ_READY=0
for i in {1..30}; do
  DLQ_READY="$(queue_metric "$PASSWORD_RESET_DLQ" messages_ready)"
  DLQ_READY="${DLQ_READY:-0}"
  if [ "$DLQ_READY" = "1" ]; then
    break
  fi
  sleep 2
done
[ "$DLQ_READY" = "1" ] || fail "sanitized terminal failure did not reach DLQ"
echo "PASSWORD_RESET_DLQ_READY=$DLQ_READY"

SOURCE_READY=1
SOURCE_UNACK=1
for i in {1..10}; do
  SOURCE_READY="$(queue_metric "$PASSWORD_RESET_QUEUE" messages_ready)"
  SOURCE_UNACK="$(queue_metric "$PASSWORD_RESET_QUEUE" messages_unacknowledged)"
  SOURCE_READY="${SOURCE_READY:-0}"
  SOURCE_UNACK="${SOURCE_UNACK:-0}"
  if [ "$SOURCE_READY" = "0" ] && [ "$SOURCE_UNACK" = "0" ]; then
    break
  fi
  sleep 1
done
echo "PASSWORD_RESET_SOURCE_READY=$SOURCE_READY"
echo "PASSWORD_RESET_SOURCE_UNACKED=$SOURCE_UNACK"
[ "$SOURCE_READY" = "0" ] && [ "$SOURCE_UNACK" = "0" ] \
  || fail "sensitive source message was left queued/requeued"
echo "SENSITIVE_SOURCE_ACK=PASS"

docker logs --since "$TEST_SINCE" "$NOTIFICATION_CONTAINER" >"$TMP_DIR/notification.log" 2>&1
ATTEMPT_COUNT=$(grep -c 'Password-reset delivery attempt failed attempt=' "$TMP_DIR/notification.log" || true)
echo "RETRY_FAILURE_LOG_COUNT=$ATTEMPT_COUNT"
[ "$ATTEMPT_COUNT" = "$EXPECTED_ATTEMPTS" ] \
  || fail "expected $EXPECTED_ATTEMPTS delivery failures, found $ATTEMPT_COUNT"

if grep -Fq 'reset-password?token=' "$TMP_DIR/notification.log" \
  || grep -Fq "$SMOKE_EMAIL" "$TMP_DIR/notification.log"; then
  fail "sensitive reset material found in notification-service logs"
fi
echo "NOTIFICATION_LOG_TOKEN_LEAK=PASS"

RABBIT_USER=$(docker exec "$RABBIT_CONTAINER" sh -lc 'printf %s "$RABBITMQ_DEFAULT_USER"')
RABBIT_PASS=$(docker exec "$RABBIT_CONTAINER" sh -lc 'printf %s "$RABBITMQ_DEFAULT_PASS"')

curl --noproxy '*' --max-time 10 -sS \
  -u "$RABBIT_USER:$RABBIT_PASS" \
  -H 'Content-Type: application/json' \
  -X POST \
  -d '{"count":1,"ackmode":"ack_requeue_true","encoding":"auto","truncate":50000}' \
  "http://127.0.0.1:15672/api/queues/%2F/${PASSWORD_RESET_DLQ}/get" \
  >"$TMP_DIR/dlq-message.json"

export DLQ_CAPTURE_WIN EXPECTED_ATTEMPTS
DLQ_CAPTURE_WIN="$(cygpath -w "$TMP_DIR/dlq-message.json")"

powershell.exe -NoProfile -Command '
  $messages = @(Get-Content -LiteralPath $env:DLQ_CAPTURE_WIN -Raw | ConvertFrom-Json)
  if ($messages.Count -ne 1) { Write-Output "DLQ_SANITIZED_PAYLOAD=FAIL_COUNT"; exit 10 }

  $payloadText = [string]$messages[0].payload
  $payload = $payloadText | ConvertFrom-Json
  $actual = @($payload.PSObject.Properties.Name | Sort-Object)
  $expected = @("attempts", "failedAt", "failureType", "recipientHash") | Sort-Object
  if (($actual -join ",") -ne ($expected -join ",")) { Write-Output "DLQ_SANITIZED_PAYLOAD=FAIL_SCHEMA"; exit 11 }
  if ([string]$payload.recipientHash -notmatch "^[0-9a-f]{64}$") { Write-Output "DLQ_SANITIZED_PAYLOAD=FAIL_HASH"; exit 12 }
  if ([int]$payload.attempts -ne [int]$env:EXPECTED_ATTEMPTS) { Write-Output "DLQ_SANITIZED_PAYLOAD=FAIL_ATTEMPTS"; exit 13 }

  $lower = $payloadText.ToLowerInvariant()
  foreach ($forbidden in @("resetlink", "reset-password?token=", "http://", "https://", "email")) {
    if ($lower.Contains($forbidden)) { Write-Output "DLQ_SANITIZED_PAYLOAD=FAIL_SECRET_FIELD"; exit 14 }
  }

  Write-Output "DLQ_SANITIZED_PAYLOAD=PASS"
  Write-Output ("DLQ_ATTEMPTS=" + [int]$payload.attempts)
' | tr -d '\r'

unset RABBIT_PASS

echo "Restoring Mailpit..."
docker start "$MAILPIT_CONTAINER" >/dev/null
MAILPIT_WAS_STOPPED=0

for i in {1..30}; do
  if [ "$(http_code http://127.0.0.1:8025/api/v1/messages)" = "200" ]; then
    break
  fi
  [ "$i" -lt 30 ] || fail "Mailpit did not recover"
  sleep 2
done
echo "MAILPIT_RESTORE=PASS"

FRESH_FORGOT_HTTP=$(curl --noproxy '*' --max-time 20 -sS \
  -o "$TMP_DIR/forgot-success.json" \
  -w '%{http_code}' \
  -X POST \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$SMOKE_EMAIL\"}" \
  http://127.0.0.1:8080/api/v1/auth/forgot-password || true)
echo "FRESH_FORGOT_HTTP=$FRESH_FORGOT_HTTP"
[ "$FRESH_FORGOT_HTTP" = "200" ] || fail "fresh forgot-password request failed after Mailpit restore"

FRESH_MAIL_COUNT="$BASELINE_MAIL_COUNT"
for i in {1..20}; do
  FRESH_MAIL_COUNT=$(mailpit_count "$TMP_DIR/mailpit-after.json")
  if [ "$FRESH_MAIL_COUNT" -gt "$BASELINE_MAIL_COUNT" ]; then
    break
  fi
  sleep 2
done

echo "MAILPIT_FINAL_RESET_COUNT=$FRESH_MAIL_COUNT"
[ "$FRESH_MAIL_COUNT" -gt "$BASELINE_MAIL_COUNT" ] \
  || fail "fresh password-reset email did not arrive after Mailpit restore"
echo "FRESH_RESET_DELIVERY=PASS"

docker exec "$RABBIT_CONTAINER" rabbitmqctl purge_queue "$PASSWORD_RESET_DLQ" >/dev/null

echo "PASSWORD_RESET_RELIABILITY_RUNTIME_GATE=PASS"
