#!/usr/bin/env bash
# Run once on the existing Amazon Linux EC2 instance, after attaching its IAM profile.
set -Eeuo pipefail
test "$(id -u)" -eq 0 || { echo 'Run with sudo bash'; exit 1; }
test -s /home/ec2-user/campus-orders/.env
export DOCKER_CONFIG=/home/ec2-user/.docker
docker compose version
docker volume inspect campus-orders_postgres_data >/dev/null
if ! rpm -q codedeploy-agent >/dev/null 2>&1; then
  dnf install -y ruby wget
  scratch=$(mktemp -d)
  trap 'rm -rf -- "$scratch"' EXIT
  curl -fsSL https://aws-codedeploy-us-east-1.s3.us-east-1.amazonaws.com/latestv2/install -o "$scratch/install"
  chmod 700 "$scratch/install"
  "$scratch/install" auto
fi
systemctl enable --now docker codedeploy-agent
systemctl is-active codedeploy-agent
