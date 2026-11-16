#!/usr/bin/env bash
set -Eeuo pipefail
source "$(dirname -- "$0")/common.sh"
test -s "$APP_HOME/.env"
test -s "$ARCHIVE_ROOT/backend/target/order-management-1.0.0.jar"
test -s "$ARCHIVE_ROOT/frontend/dist/index.html"
test -s "$ARCHIVE_ROOT/frontend/dist/version.json"
docker compose version
docker info >/dev/null
docker volume inspect campus-orders_postgres_data >/dev/null
test "$(docker inspect --format '{{.State.Health.Status}}' campus-orders-db-1)" = healthy
# Keep the original database and fail before changing containers if backup fails.
install -d -m 700 /var/backups/campus-orders
umask 077
docker exec campus-orders-db-1 sh -c 'exec pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  | gzip > "/var/backups/campus-orders/$DEPLOYMENT_ID.sql.gz"
gzip -t "/var/backups/campus-orders/$DEPLOYMENT_ID.sql.gz"
test "$(df --output=avail -k /opt | tail -1)" -gt 2097152 || {
  echo 'At least 2 GiB free disk required before deploying'; exit 1;
}
echo 'Preflight and local database backup completed.'
