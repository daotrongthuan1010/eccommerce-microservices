#!/bin/bash
# Step 6: Pull fix gateway lb + recreate gateway + smoke RBAC day du.
# Chay: bash /tmp/vps-step6-smoke.sh
set -e
cd /opt/eccommerce
git pull origin develop 2>&1 | tail -1
docker compose --profile apps up -d api-gateway 2>&1 | tail -2
sleep 45
docker compose --profile apps ps --format 'table {{.Service}}\t{{.State}}\t{{.Health}}' | head -8
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-Admin123!}"
STUDENT_USER="${STUDENT_USER:-student}"
STUDENT_PASS="${STUDENT_PASS:-Student123!}"
echo "=== LOGIN ADMIN VIA GATEWAY ==="
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token','NO_TOKEN'))")
echo "token head: ${TOKEN:0:30}..."
echo "=== ME admin (phai 200 + roles ADMIN,USER) ==="
curl -s http://localhost:8080/api/user/me -H "Authorization: Bearer $TOKEN"; echo ""
echo "=== ADMIN-ME admin (phai 200) ==="
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/auth/admin/me -H "Authorization: Bearer $TOKEN"
echo "=== ADMIN-ME student (phai 403) ==="
STOK=$(curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$STUDENT_USER\",\"password\":\"$STUDENT_PASS\"}" | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token','NO_TOKEN'))")
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/auth/admin/me -H "Authorization: Bearer $STOK"
echo "=== ME khong token (phai 401) ==="
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/user/me
echo "SMOKE_DONE"
