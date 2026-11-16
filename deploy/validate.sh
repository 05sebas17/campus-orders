#!/usr/bin/env bash
set -Eeuo pipefail
source "$(dirname -- "$0")/common.sh"
bash "$ARCHIVE_ROOT/deploy/check-health.sh" "$ARCHIVE_ROOT/frontend/dist/version.json"
compose_release "$RELEASE" ps
