#!/bin/bash
# Trien khai ha tang eccommerce len VPS (chay tren VPS, khong chua password trong repo).
set -e
APP_DIR=/opt/eccommerce
echo "=== [1/6] Giai nen code ==="
rm -rf "$APP_DIR"
mkdir -p "$APP_DIR"
tar -xzf /tmp/eccommerce-repo.tar.gz -C "$APP_DIR"
cd "$APP_DIR"
echo "=== [2/6] Tao .env tu .env.example neu chua co ==="
if [ ! -f .env ]; then
  cp .env.example .env
  # Doi host Kafka tu localhost sang IP public VPS de dev local ket noi duoc
  sed -i 's/^KAFKA_EXTERNAL_HOST=.*/KAFKA_EXTERNAL_HOST=162.4.177.91/' .env
  echo ".env da tao, hay doi cac password mac dinh truoc khi production!"
fi
echo "=== [3/6] Pull image ha tang ==="
docker compose pull
echo "=== [4/6] Dung ha tang ==="
docker compose up -d
echo "=== [5/6] Cho ha tang healthy (toi da ~5 phut) ==="
for i in $(seq 1 30); do
  echo "--- Lan kiem tra $i/30 ---"
  docker compose ps
  UNHEALTHY=$(docker compose ps --format json 2>/dev/null | grep -c -iE '"health"\s*:\s*"(starting|unhealthy)"' || true)
  echo "Container chua healthy: $UNHEALTHY"
  if [ "$UNHEALTHY" = "0" ]; then
    echo "Ha tang da healthy!"
    break
  fi
  sleep 10
 done
echo "=== [6/6] Trang thai cuoi ==="
docker compose ps -a
echo "DEPLOY_INFRA_DONE"
