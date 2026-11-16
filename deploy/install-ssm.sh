#!/usr/bin/env bash
# Run once on Amazon Linux after associating the project instance profile.
set -Eeuo pipefail
test "$(id -u)" -eq 0 || { echo 'Run with sudo bash'; exit 1; }
test -s /home/ec2-user/campus-orders/.env
export DOCKER_CONFIG=/home/ec2-user/.docker
docker compose version
docker volume inspect campus-orders_postgres_data >/dev/null
if ! rpm -q amazon-ssm-agent >/dev/null 2>&1; then
  dnf install -y amazon-ssm-agent
fi
systemctl enable --now amazon-ssm-agent
systemctl restart amazon-ssm-agent
systemctl is-active amazon-ssm-agent
