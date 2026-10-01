#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v curl >/dev/null
docker compose config --quiet
docker compose exec -T postgres psql -U postgres -d postgres -v ON_ERROR_STOP=1 -c 'SELECT 1'
docker compose exec -T mysql sh -ec 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=TCP -h127.0.0.1 -uroot -e "SELECT 1"'
docker compose exec -T redis sh -ec 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli ping | grep -q PONG'
docker compose exec -T kafka kafka-topics --bootstrap-server kafka:29092 --list
curl -fsS 'http://localhost:9200/_cluster/health?wait_for_status=yellow&timeout=10s'
curl -fsS http://localhost:9000/minio/health/live
docker compose run --rm --no-deps --entrypoint /bin/sh minio-init -ec 'mc alias set local http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null; mc stat local/product-assets'
for port in {8080..8088}; do
  printf '\nChecking service on port %s\n' "$port"
  body=$(curl -fsS "http://localhost:$port/actuator/health")
  printf '%s\n' "$body"
  [[ "$body" == *'"status":"UP"'* ]]
done
printf '\nAll infrastructure and application checks passed.\n'
