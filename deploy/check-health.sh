#!/usr/bin/env bash
set -Eeuo pipefail
expected="${1:?Expected version.json path is required}"
base_url="${2:-http://127.0.0.1}"
for attempt in $(seq 1 12); do
  if curl -fsS --max-time 10 "$base_url/health" >/dev/null \
    && curl -fsS --max-time 10 "$base_url/api/v1/products?size=1" >/dev/null \
    && curl -fsS --max-time 10 "$base_url/api/v1/orders?size=1" >/dev/null \
    && curl -fsS --max-time 10 "$base_url/version.json" | cmp -s "$expected" -; then
    echo 'Frontend, API, database queries and deployed revision verified.'
    exit 0
  fi
  sleep 5
done
echo 'Application health or revision validation failed.' >&2
exit 1
