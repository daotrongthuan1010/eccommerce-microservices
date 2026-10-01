# Hạ tầng dự án Ecommerce Microservices — Cách dùng & truy cập từ trình duyệt qua IP:Port

> Đối tượng: dev chạy local + VPS `162.4.177.91` (SSH `ssh -p 24700 -i C:/Users/ADMIN/.ssh/vps_eccommerce root@162.4.177.91`).
> Ngày cập nhật: 01/10/2026. Nguồn thật: `docker-compose.yml`, `scripts/*.sh`, `.env.example`, `docs/OPERATIONS.md`.

---

## 1. Tổng quan kiến trúc

```
Browser (máy bạn)
   |
   |  http://<VPS-IP>:<port>  (sau khi mở firewall + bind 0.0.0.0, xem mục 4)
   v
VPS 162.4.177.91  (/opt/eccommerce, Docker + Compose v5)
   |
   +-- Hạ tầng (profile mặc định, `docker compose up -d`): postgres, mysql, redis,
   |      kafka, elasticsearch, minio, keycloak, kafka-ui, redis-commander,
   |      prometheus, grafana, zipkin (+ job minio-init, kafka-init)
   +-- Apps (profile `apps`, `docker compose --profile apps up -d`): discovery-service,
          api-gateway, auth-service, user-service, catalog-service,
          inventory-service, order-service, payment-service,
          notification-service, search-service
```

- Code sửa dưới **local** (`D:\study-dev\eccommerce-microservices`), đồng bộ lên VPS mỗi lần sửa (scp/rsync/tar như `scripts/deploy-vps-infra.sh`).
- Mật khẩu nằm ở `.env` (copy từ `.env.example`, **không commit**). Đổi `.env` không đổi mật khẩu đã lưu trong volume DB — xem `docs/OPERATIONS.md` mục "Mật khẩu và volume".
- Kafka có 2 listener: `kafka:29092` (trong container) và `<KAFKA_EXTERNAL_HOST>:9092` (từ host). Trên VPS `.env` phải là `KAFKA_EXTERNAL_HOST=162.4.177.91` (các script `vps-step1-infra.sh`, `deploy-vps-infra.sh` đã tự `sed` việc này).

## 2. Bảng port đầy đủ (nguồn: `docker-compose.yml`)

> Cột "Publish hiện tại" sau fix 01/10/2026 đã là **`0.0.0.0` (mở public toàn bộ)** để phục vụ học/dev.
> File `docker-compose.public.yml` override trước đây đã **bỏ, không dùng nữa** — nếu VPS còn file này thì xoá đi
> (`rm /opt/eccommerce/docker-compose.public.yml`) vì nó gây mất port khi merge. UFW trên VPS đang `inactive`
> nên không cần mở firewall OS; chỉ cần recreate stack là browser vào được ngay.

### 2.1. Apps (Java, profile `apps`)

| Service | Port container | Publish hiện tại | Nên mở public? | URL sau khi mở |
|---|---|---|---|---|
| discovery-service (Eureka) | 8761 | `0.0.0.0:8761:8761` | Có (dev) | `http://162.4.177.91:8761` |
| api-gateway | 8080 | `0.0.0.0:8080:8080` | **Có** — cửa ngõ chính | `http://162.4.177.91:8080/actuator/health` |
| auth-service | 8081 | `0.0.0.0:8081:8081` | Có (debug trực tiếp) | `http://162.4.177.91:8081/actuator/health` |
| user-service | 8082 | `0.0.0.0:8082:8082` | Có (debug trực tiếp) | `http://162.4.177.91:8082/actuator/health` |
| catalog-service | 8083 | `0.0.0.0:8083:8083` | Có (debug trực tiếp) | `http://162.4.177.91:8083/actuator/health` |
| inventory-service | 8084 | `0.0.0.0:8084:8084` | Có (debug trực tiếp) | `http://162.4.177.91:8084/actuator/health` |
| order-service | 8085 | `0.0.0.0:8085:8085` | Có (debug trực tiếp) | `http://162.4.177.91:8085/actuator/health` |
| payment-service | 8086 | `0.0.0.0:8086:8086` | Có (debug trực tiếp) | `http://162.4.177.91:8086/actuator/health` |
| notification-service | 8087 | `0.0.0.0:8087:8087` | Có (debug trực tiếp) | `http://162.4.177.91:8087/actuator/health` |
| search-service | 8088 | `0.0.0.0:8088:8088` | Có (debug trực tiếp) | `http://162.4.177.91:8088/actuator/health` |

Route qua gateway (`services/api-gateway/src/main/resources/application.yml`, `StripPrefix=2`):

| Gọi từ browser | Gateway chuyển tiếp tới |
|---|---|
| `GET http://<IP>:8080/api/auth/**` | `auth-service` |
| `GET http://<IP>:8080/api/user/**` | `user-service` |
| `GET http://<IP>:8080/api/catalog/**` | `catalog-service` |
| `GET http://<IP>:8080/api/inventory/**` | `inventory-service` |
| `GET http://<IP>:8080/api/order/**` | `order-service` |
| `GET http://<IP>:8080/api/payment/**` | `payment-service` |
| `GET http://<IP>:8080/api/notification/**` | `notification-service` |
| `GET http://<IP>:8080/api/search/**` | `search-service` |

### 2.2. Hạ tầng & quan sát (profile mặc định)

| Hạ tầng | Publish hiện tại | Nên mở public? | URL sau khi mở |
|---|---|---|---|
| Keycloak | `0.0.0.0:7080:8080` | **Có** (để login/test realm) | `http://162.4.177.91:7080` và `http://162.4.177.91:7080/realms/ecommerce/.well-known/openid-configuration` |
| MinIO API | `0.0.0.0:9000:9000` | Có (upload/test) | `http://162.4.177.91:9000` |
| MinIO Console | `0.0.0.0:9001:9001` | **Có** (UI xem bucket `product-assets`) | `http://162.4.177.91:9001` (user/pass trong `.env`: `MINIO_ROOT_USER/PASSWORD`) |
| Kafka (external) | `0.0.0.0:9092:9092` | Có (dev local nối Kafka VPS) | `162.4.177.91:9092` (dùng với `KAFKA_EXTERNAL_HOST=162.4.177.91`) |
| Elasticsearch | `0.0.0.0:9200:9200` | Có (dev) | `http://162.4.177.91:9200/_cluster/health` |
| Prometheus | `0.0.0.0:9090:9090` | **Có** (UI metrics) | `http://162.4.177.91:9090` |
| Grafana | `0.0.0.0:3000:3000` | **Có** (dashboard) | `http://162.4.177.91:3000` (user/pass: `GRAFANA_ADMIN_USER/PASSWORD` trong `.env`) |
| Zipkin | `0.0.0.0:9411:9411` | Có (trace UI) | `http://162.4.177.91:9411` |
| Kafka-UI | `0.0.0.0:8090:8080` | **Có** (UI xem topic) | `http://162.4.177.91:8090` |
| redis-commander | `0.0.0.0:8091:8081` | Có (UI xem Redis) | `http://162.4.177.91:8091` |
| PostgreSQL | `0.0.0.0:5432:5432` | Có (học/dev — nối trực tiếp từ IDE, user/pass trong `.env`) | `162.4.177.91:5432` (host, user `postgres` / `catalog_user`... tùy DB) |
| MySQL | `0.0.0.0:3306:3306` | Có (học/dev — nối trực tiếp từ IDE, user/pass trong `.env`) | `162.4.177.91:3306` (host, user `root` / `auth_user`... tùy DB) |
| Redis | `0.0.0.0:6379:6379` | Có (học/dev — nối trực tiếp, pass `REDIS_PASSWORD` trong `.env`) | `162.4.177.91:6379` |

> Lưu ý bảo mật: mở DB/Redis/Kafka/Elasticsearch ra public chỉ chấp nhận được vì đây là **hạ tầng học/dev**,
> không có dữ liệu thật. Khi lên môi trường có dữ liệu thật thì đóng lại các port `5432, 3306, 6379, 9092, 9200`
> (chuyển bind về `127.0.0.1` + dùng SSH tunnel ở mục 5.3).

## 3. Cách dùng (chuẩn theo scripts trong repo)

### 3.1. Lần đầu trên VPS

```bash
# Trên VPS /opt/eccommerce
cp .env.example .env
sed -i 's/^KAFKA_EXTERNAL_HOST=.*/KAFKA_EXTERNAL_HOST=162.4.177.91/' .env
# Đổi mọi password mẫu trong .env trước khi public!

# Hạ tầng trước (script vps-step1-infra.sh)
docker compose up -d postgres mysql redis kafka elasticsearch minio minio-init kafka-init \
  keycloak kafka-ui redis-commander prometheus grafana zipkin

# Apps sau (script vps-step3-apps.sh / vps-step4-rbac.sh)
docker compose --profile apps build discovery-service api-gateway auth-service user-service
docker compose --profile apps up -d discovery-service api-gateway auth-service user-service
```

### 3.2. Vận hành hằng ngày

```bash
cd /opt/eccommerce
docker compose ps                                  # hạ tầng
docker compose --profile apps ps                   # apps
docker compose logs --tail=100 catalog-service
docker compose --profile apps logs --tail=100 api-gateway

# Kiểm tra nhanh (scripts/smoke.sh cho local, scripts/vps-step5-smoke.sh cho RBAC qua gateway)
curl -fsS http://localhost:8080/actuator/health
bash scripts/vps-step5-smoke.sh
```

### 3.3. Login/test RBAC qua gateway (từ `scripts/vps-step5-smoke.sh`)

```bash
# Login lấy token (user mặc định từ infra/keycloak/realm.json, dev only)
curl -s -X POST http://162.4.177.91:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin123!"}'

# Gọi API kèm token
TOKEN=...  # access_token ở trên
curl -s http://162.4.177.91:8080/api/user/me -H "Authorization: Bearer $TOKEN"
curl -s http://162.4.177.91:8080/api/auth/admin/me -H "Authorization: Bearer $TOKEN"
# Kỳ vọng: admin token -> 200; student token gọi /admin/me -> 403; không token -> 401
```

## 4. Vấn đề: vì sao chưa truy cập từ trình duyệt qua IP:port được

1. **Compose bind `127.0.0.1`** (ví dụ `- 127.0.0.1:8080:8080`): cổng chỉ nghe loopback của VPS, gói tin từ ngoài không bao giờ tới container. Đây là cấu hình **đúng cho local**, **sai cho VPS muốn public**.
2. **Firewall của VPS (UFW/iptables) chưa mở port** → browser timeout.
3. **Firewall của nhà cung cấp cloud (Security Group)** nếu có → cũng phải mở.

### 4.1. Cách mở đúng (hạ tầng học/dev — đã áp dụng 01/10/2026): public toàn bộ, bind thẳng `0.0.0.0` trong `docker-compose.yml`,

**Cách mở đúng (hạ tầng học/dev — đã áp dụng 01/10/2026): bind thẳng `0.0.0.0` trong `docker-compose.yml`,
không dùng file override. UFW đang `inactive` nên không cần rule firewall OS.**

```bash
cd /opt/eccommerce
rm -f docker-compose.public.yml   # file override cũ gây mất port khi merge, bỏ
docker compose --profile apps up -d
ss -tln | grep -E '8080|8761'     # phải thấy 0.0.0.0:8080, 0.0.0.0:8761...
```

> Ghi chú dev: compose merge 2 file bằng cách **nối** danh sách `ports` chứ không thay thế,
> nên file override cũ đã gây lỗi `address already in use` và rớt mất port khi recreate.
> Vì vậy fix triệt để bằng cách sửa bind ngay trong `docker-compose.yml` gốc.
> Chi tiết xem `docker-compose.yml` (tất cả `- 0.0.0.0:<port>:<port>`).

**Bước 2 — Nếu sau này bật UFW / nhà cung cấp có Security Group:** mở cùng danh sách port TCP
(8080, 8761, 7080, 8090, 8091, 9090, 3000, 9411, 9000, 9001).

**Bước 4 — Kiểm tra từ máy bạn:**

```powershell
Invoke-RestMethod http://162.4.177.91:8080/actuator/health
Invoke-RestMethod http://162.4.177.91:8761/actuator/health
start http://162.4.177.91:3000      # grafana
start http://162.4.177.91:8090      # kafka-ui
start http://162.4.177.91:7080      # keycloak
```

### 4.2. Kiểm tra nhanh khi vẫn không vào được

```bash
# Trên VPS: port có nghe 0.0.0.0 không? (phải thấy 0.0.0.0:8080, không phải 127.0.0.1:8080)
ss -tlnp | grep -E '8080|8761|7080|8090|3000|9090|9411|9000|9001'
# Trong VPS gọi localhost phải OK trước
curl -fsS http://localhost:8080/actuator/health
# Từ máy bạn test port TCP (PowerShell)
Test-NetConnection 162.4.177.91 -Port 8080
```

| Triệu chứng | Nguyên nhân likely | Xử lý |
|---|---|---|
| `localhost` trên VPS OK, từ ngoài timeout | Chưa override `0.0.0.0` hoặc UFW/Security Group chặn | Làm lại mục 4.1 bước 1–3, kiểm tra `ss -tlnp` |
| `curl localhost` cũng fail | Container chưa healthy | `docker compose --profile apps ps`, xem `logs` |
| Keycloak redirect sai host | `KC_HOSTNAME_STRICT=false` đang tắt strict nên OK dev; prod cần set hostname chuẩn | Giữ nguyên cho dev |
| Kafka dev local không nối được | `KAFKA_EXTERNAL_HOST` vẫn `localhost` | `grep KAFKA_EXTERNAL_HOST .env`, phải là IP VPS |

## 5. Nối IDE trực tiếp (không cần SSH tunnel — hạ tầng học/dev public toàn bộ)

```bash
# Dù bind đã là 0.0.0.0, SSH tunnel vẫn dùng được nếu muốn:
ssh -p 24700 -i C:/Users/ADMIN/.ssh/vps_eccommerce -L 8080:localhost:8080 -L 3000:localhost:3000 root@162.4.177.91
```

### 5.1. Nối IDE (IntelliJ/DataGrip) tới DB trên VPS — nối thẳng

| DB | Host | Port | User | Pass lấy ở |
|---|---|---|---|---|
| PostgreSQL | `162.4.177.91` | 5432 | `postgres` / `catalog_user`... tùy DB | `.env` trên VPS |
| MySQL | `162.4.177.91` | 3306 | `root` / `auth_user`... tùy DB | `.env` trên VPS |
| Redis | `162.4.177.91` | 6379 | (pass) | `REDIS_PASSWORD` trong `.env` |
| Kafka | `162.4.177.91` | 9092 | — | `KAFKA_EXTERNAL_HOST=162.4.177.91` |

### 5.2. Reverse proxy + HTTPS (khi lên prod)

Đặt Nginx/Caddy trước gateway + Keycloak, chỉ mở 80/443, bật TLS, Keycloak dùng `KC_HOSTNAME` thật. Cấu hình compose hiện tại (`KC_PROXY_HEADERS=xforwarded`) đã hỗ trợ chạy sau proxy.

### 5.3. SSH tunnel (dự phòng, khi cần)

```
DB host trong IDE: localhost, port: 5432 (postgres) / 3306 (mysql)
SSH: host 162.4.177.91 port 24700 user root key vps_eccommerce
```

## 6. Checklist sau khi mở public

- [x] `docker-compose.yml` bind `0.0.0.0` toàn bộ + `ss -tln` thấy `0.0.0.0` — xong 01/10/2026
- [x] UFW đang `inactive` nên không cần rule firewall OS
- [x] `http://162.4.177.91:8080/actuator/health` → `200` từ máy local — xong 01/10/2026
- [ ] `.env` đổi hết password mẫu (`grep -i change_me .env` không còn gì) — nên làm
- [ ] Login qua gateway + phân quyền 200/403/401 đúng như mục 3.3

## 7. Tài liệu liên quan trong repo

- `README.md` — chạy local, địa chỉ `localhost`, chạy trong IntelliJ.
- `docs/OPERATIONS.md` — kiểm tra hạ tầng trực tiếp, đổi mật khẩu/volume, lỗi thường gặp.
- `docs/VALIDATION.md` — phạm vi đã kiểm tra tĩnh (chưa xác nhận chạy thực tế lúc dựng).
- `scripts/smoke.sh` — smoke local (localhost).
- `scripts/vps-step1-infra.sh` → `vps-step5-smoke.sh` — quy trình chuẩn trên VPS theo từng bước.
- `infra/prometheus/prometheus.yml` — job scrape `spring-apps` đang trỏ service-name nội bộ (chỉ đúng trong network compose).
- `docs\OBSERVABILITY-GUIDE.md` — hướng dẫn dùng Grafana, Prometheus, Zipkin, Kafka-UI, Redis Commander, Eureka, Keycloak, MinIO, Elasticsearch + kịch bản học 15 phút.
