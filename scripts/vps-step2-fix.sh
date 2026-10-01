#!/bin/bash
# Step 2: Pull code moi + fix compose + rebuild ha tang. Chay: bash /tmp/vps-step2-fix.sh
set -e
cd /opt/eccommerce
echo "=== [1/4] Pull code ==="
git pull origin develop
echo "=== [2/4] Recreate keycloak + redis-commander voi compose moi ==="
docker compose up -d keycloak redis-commander
echo "=== [3/4] Cho keycloak healthy (toi da ~3 phut) ==="
for i in $(seq 1 18); do
  KC=$(docker inspect eccommerce-keycloak-1 --format '{{.State.Health.Status}}' 2>/dev/null || echo "?")
  RC=$(docker inspect eccommerce-redis-commander-1 --format '{{.State.Status}}' 2>/dev/null || echo "?")
  echo "lan $i/18: keycloak=$KC redis-commander=$RC"
  if [ "$KC" = "healthy" ] && [ "$RC" = "running" ]; then echo "CA 2 OK"; break; fi
  sleep 10
done
echo "=== [4/4] Kiem tra nhanh ==="
curl -fsS http://localhost:9000/health/ready && echo " keycloak-ready-OK"
curl -fsS http://localhost:7080/realms/ecommerce/.well-known/openid-configuration | head -c 200; echo ""
docker compose ps keycloak redis-commander
echo "STEP2_DONE"
