#!/bin/bash
# Step 4: Pull RBAC + build 4 service + smoke test auth. Chay: bash /tmp/vps-step4-rbac.sh
set -e
cd /opt/eccommerce
echo "=== [1/5] Pull code ==="
git pull origin develop
echo "=== [2/5] Build 4 service ==="
docker compose --profile apps build discovery-service api-gateway auth-service user-service 2>&1 | tail -n 8
echo "=== [3/5] Up 4 service ==="
docker compose --profile apps up -d discovery-service api-gateway auth-service user-service
echo "=== [4/5] Cho service len (90s) ==="
sleep 90
docker compose --profile apps ps --format "table {{.Service}}\t{{.State}}\t{{.Health}}"
echo "=== [5/5] Smoke test ==="
echo "--- discovery :8761 ---"
curl -fsS http://localhost:8761/actuator/health 2>&1 | head -c 200; echo ""
echo "--- login admin via gateway :8080/api/auth/login ---"
TOKEN=$(curl -fsS -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"Admin123!"}' 2>&1 | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token','NO_TOKEN'))" 2>&1)
echo "token: ${TOKEN:0:40}..."
if [ "$TOKEN" != "NO_TOKEN" ] && [ -n "$TOKEN" ]; then
  echo "--- /api/user/me voi token admin ---"
  curl -fsS http://localhost:8080/api/user/me -H "Authorization: Bearer $TOKEN" 2>&1 | head -c 400; echo ""
  echo "--- /api/auth/admin/me voi token admin (phai 200) ---"
  curl -fsS http://localhost:8080/api/auth/admin/me -H "Authorization: Bearer $TOKEN" 2>&1 | head -c 300; echo ""
fi
echo "STEP4_DONE"
