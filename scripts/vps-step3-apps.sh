#!/bin/bash
# Step 3: Pull fix + recreate keycloak + build 4 service + smoke test. Chay: bash /tmp/vps-step3-apps.sh
set -e
cd /opt/eccommerce
echo "=== [1/6] Pull code ==="
git pull origin develop
echo "=== [2/6] Recreate keycloak ==="
docker compose up -d keycloak
sleep 5
for i in $(seq 1 12); do
  KC=$(docker inspect eccommerce-keycloak-1 --format '{{.State.Health.Status}}' 2>/dev/null || echo "?")
  echo "lan $i/12: keycloak=$KC"
  if [ "$KC" = "healthy" ]; then echo "KEYCLOAK HEALTHY"; break; fi
  sleep 10
done
echo "=== [3/6] Build 4 service (discovery/gateway/auth/user) ==="
docker compose --profile apps build discovery-service api-gateway auth-service user-service
echo "=== [4/6] Up 4 service ==="
docker compose --profile apps up -d discovery-service api-gateway auth-service user-service
echo "=== [5/6] Cho service healthy (toi da ~5 phut) ==="
for i in $(seq 1 30); do
  echo "--- lan $i/30 ---"
  docker compose --profile apps ps --format "table {{.Service}}\t{{.State}}\t{{.Health}}"
  sleep 10
done
echo "=== [6/6] Smoke test ==="
for port in 8761 8080 8081 8082; do
  echo "--- :$port/actuator/health ---"
  curl -fsS http://localhost:$port/actuator/health 2>&1 | head -c 300; echo ""
done
echo "STEP3_DONE"
