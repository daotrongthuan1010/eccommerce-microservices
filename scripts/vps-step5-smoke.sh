#!/bin/bash
# Smoke test RBAC qua Gateway. Dung user/pass mac dinh tu realm.json (dev only).
# Chay: bash /tmp/vps-step5-smoke.sh
set -e
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-Admin123!}"
STUDENT_USER="${STUDENT_USER:-student}"
STUDENT_PASS="${STUDENT_PASS:-Student123!}"
echo "=== LOGIN ADMIN ==="
curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" | head -c 300; echo ""
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token','NO_TOKEN'))")
echo "token head: ${TOKEN:0:30}..."
echo "=== ME VIA GATEWAY (phai 200) ==="
curl -s http://localhost:8080/api/user/me -H "Authorization: Bearer $TOKEN" | head -c 500; echo ""
echo "=== ADMIN-ME admin token (phai 200) ==="
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/auth/admin/me -H "Authorization: Bearer $TOKEN"
echo "=== ADMIN-ME student token (phai 403) ==="
STOK=$(curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$STUDENT_USER\",\"password\":\"$STUDENT_PASS\"}" | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token','NO_TOKEN'))")
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/auth/admin/me -H "Authorization: Bearer $STOK"
echo "=== ME khong token (phai 401) ==="
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/user/me
echo "SMOKE_DONE"
