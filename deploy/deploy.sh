#!/usr/bin/env bash
set -Eeuo pipefail
source "$(dirname -- "$0")/common.sh"
install -d -m 755 "$DEPLOY_HOME/releases" "$RELEASE"
exec 9>"$DEPLOY_HOME/deploy.lock"
flock -n 9 || { echo 'Another deployment is running'; exit 1; }
cp -a "$ARCHIVE_ROOT/backend" "$ARCHIVE_ROOT/frontend" "$ARCHIVE_ROOT/deploy" "$RELEASE/"
printf 'BACKEND_IMAGE=campus-orders-backend:%s\nFRONTEND_IMAGE=campus-orders-frontend:%s\n' \
  "$DEPLOYMENT_ID" "$DEPLOYMENT_ID" > "$RELEASE/images.env"
docker build -f "$RELEASE/deploy/backend.Dockerfile" -t "campus-orders-backend:$DEPLOYMENT_ID" "$RELEASE"
docker build -f "$RELEASE/deploy/frontend.Dockerfile" -t "campus-orders-frontend:$DEPLOYMENT_ID" "$RELEASE"
compose_release "$RELEASE" config --quiet
PREVIOUS="$(readlink -f "$DEPLOY_HOME/current" 2>/dev/null || true)"
rollback() {
  local status=$?
  trap - ERR
  echo 'Deployment failed. Restoring previous application containers; database volume is preserved.'
  if [[ -n "$PREVIOUS" && -f "$PREVIOUS/images.env" ]]; then
    compose_release "$PREVIOUS" up -d --no-build --no-deps --wait --wait-timeout 240 backend frontend || true
  else
    docker compose --project-name campus-orders --env-file "$APP_HOME/.env" \
      -f "$APP_HOME/compose.yaml" up -d --no-build --no-deps --wait --wait-timeout 240 backend frontend || true
  fi
  exit "$status"
}
trap rollback ERR
# Do not recreate PostgreSQL or run compose down. Use the existing database.
docker update --restart unless-stopped campus-orders-db-1 >/dev/null
compose_release "$RELEASE" up -d --no-build --no-deps --wait --wait-timeout 240 backend frontend
bash "$RELEASE/deploy/check-health.sh" "$RELEASE/frontend/dist/version.json"
ln -sfn "$RELEASE" "$DEPLOY_HOME/current"
trap - ERR
echo "Deployment $DEPLOYMENT_ID completed."
