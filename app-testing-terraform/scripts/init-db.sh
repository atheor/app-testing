#!/usr/bin/env bash
# =============================================================
# init-db.sh — Wait for PostgreSQL to be ready then verify schema
# =============================================================
# This script is a convenience helper; Flyway (bundled in the Lambda)
# runs the actual SQL migrations automatically on the first start.
# =============================================================
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-atheor}"
DB_USER="${DB_USER:-atheor}"
DB_PASS="${DB_PASS:-atheor123}"

echo "===> Waiting for PostgreSQL at ${DB_HOST}:${DB_PORT}..."

MAX_RETRIES=30
RETRY=0
until PGPASSWORD="${DB_PASS}" psql -h "${DB_HOST}" -p "${DB_PORT}" \
        -U "${DB_USER}" -d "${DB_NAME}" -c '\q' 2>/dev/null; do
  RETRY=$((RETRY + 1))
  if [ "${RETRY}" -ge "${MAX_RETRIES}" ]; then
    echo "ERROR: PostgreSQL did not become available after ${MAX_RETRIES} attempts."
    exit 1
  fi
  echo "  Attempt ${RETRY}/${MAX_RETRIES} — retrying in 2 seconds..."
  sleep 2
done

echo "===> PostgreSQL is ready."

echo "===> Verifying tables (created by Flyway on Lambda start)..."
PGPASSWORD="${DB_PASS}" psql -h "${DB_HOST}" -p "${DB_PORT}" \
    -U "${DB_USER}" -d "${DB_NAME}" \
    -c "\dt"
