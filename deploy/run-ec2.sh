#!/usr/bin/env bash
# PostScript for the native CodePipeline EC2 action (executed through SSM).
set -Eeuo pipefail
test "$(id -u)" -eq 0 || { echo 'The deployment must run as root through SSM'; exit 1; }
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# Each attempt gets a separate directory and image tag, including retries of a commit.
deployment_uuid="$(cat /proc/sys/kernel/random/uuid)"
export DEPLOYMENT_ID="d-${deployment_uuid//-/}"
bash "$script_dir/preflight.sh"
bash "$script_dir/deploy.sh"
