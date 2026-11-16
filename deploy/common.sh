#!/usr/bin/env bash
set -Eeuo pipefail
export PATH="/usr/local/bin:/usr/bin:/bin:$PATH"
export DOCKER_CONFIG=/home/ec2-user/.docker
APP_HOME=/home/ec2-user/campus-orders
DEPLOY_HOME=/opt/campus-orders
ARCHIVE_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
: "${DEPLOYMENT_ID:?Run this hook through CodeDeploy}"
[[ "$DEPLOYMENT_ID" =~ ^d-[A-Za-z0-9]+$ ]] || { echo 'Invalid deployment ID'; exit 1; }
RELEASE="$DEPLOY_HOME/releases/$DEPLOYMENT_ID"
compose_release() {
  local directory="$1"
  shift
  docker compose --project-name campus-orders --env-file "$APP_HOME/.env" \
    --env-file "$directory/images.env" -f "$directory/deploy/compose.aws.yaml" "$@"
}
