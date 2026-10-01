#!/bin/bash
# Setup .env + ha tang tren VPS. Chay: bash /tmp/vps-step1-infra.sh
set -e
cd /opt/eccommerce
echo "=== [1/4] Tao .env ==="
if [ ! -f .env ]; then
  cp .env.example .env
  sed -i 's/^KAFKA_EXTERNAL_HOST=.*/KAFKA_EXTERNAL_HOST=162.4.177.91/' .env
  echo ".env da tao tu .env.example (KAFKA_EXTERNAL_HOST=162.4.177.91)"
else
  echo ".env da ton tai, giu nguyen"
fi
echo "=== [2/4] Pull image ha tang ==="
docker compose pull postgres mysql redis kafka elasticsearch minio minio-init kafka-init keycloak kafka-ui redis-commander prometheus grafana zipkin
echo "=== [3/4] Up ha tang ==="
docker compose up -d postgres mysql redis kafka elasticsearch minio minio-init kafka-init keycloak kafka-ui redis-commander prometheus grafana zipkin
echo "=== [4/4] Cho healthy (toi da ~6 phut) ==="
for i in $(seq 1 36); do
  echo "--- lan $i/36 ---"
  docker compose ps --format "table {{.Service}}\t{{.State}}\t{{.Health}}"
  NOTREADY=$(docker compose ps postgres mysql redis kafka elasticsearch minio keycloak --format json 2>/dev/null | grep -c -iE '"health"\s*:\s*"(starting|unhealthy)"|"state"\s*:\s*"(created|restarting|exited|dead)"' || true)
  echo "chua san sang: $NOTREADY"
  if [ "$NOTREADY" = "0" ]; then echo "HA TANG SAN SANG"; break; fi
  sleep 10
done
docker compose ps -a
echo "STEP1_DONE"
