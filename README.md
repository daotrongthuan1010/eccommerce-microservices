# Ecommerce microservices — k8s fullstack (VPS 162.4.177.91)

Khung dự án Java 21, Maven multi-module, Spring Boot 3.5.6, Spring Cloud 2025.0.0.
Runtime duy nhất: **k3s** namespace `ecommerce` trên VPS `162.4.177.91`
(SSH `ssh -p 24700 -i C:/Users/ADMIN/.ssh/vps_eccommerce root@162.4.177.91`).
Docker Compose đã cắt hoàn toàn (Phase 3, 22/10/2026) — volumes compose cũ được
giữ 7 ngày trên VPS rồi xóa. Không còn `docker-compose.yml` trong repo.

## Phân chia service và dữ liệu

| Project | Cổng | Hạ tầng dữ liệu | Database/user |
|---|---:|---|---|
| api-gateway | 8080 | Gateway WebFlux; không DB | — |
| auth-service | 8081 | MySQL | auth_db / auth_user |
| user-service | 8082 | MySQL | user_db / user_user |
| catalog-service | 8083 | PostgreSQL, MinIO | catalog_db / catalog_user |
| inventory-service | 8084 | PostgreSQL | inventory_db / inventory_user |
| order-service | 8085 | PostgreSQL | order_db / order_user |
| payment-service | 8086 | MySQL | payment_db / payment_user |
| notification-service | 8087 | PostgreSQL | notification_db / notification_user |
| search-service | 8088 | Elasticsearch | Chưa tạo index/mapping |
| product-service | 8092 | PostgreSQL, MinIO | product_db / product_user |
| review-service | 8093 | PostgreSQL | review_db / review_user |
| analytics-service | 8094 | PostgreSQL | analytics_db / analytics_user |
| file-service | 8095 | MinIO, ClamAV | — |
| discovery-service | 8761 | Eureka server | — |

Tất cả service backend đã có dependency và cấu hình Kafka/Redis. Không dùng DB chung
giữa các service; các DB riêng dùng chung instance MySQL/PostgreSQL. Search dùng
Elasticsearch làm kho tìm kiếm, không thay thế dữ liệu giao dịch của catalog/order.

## Deploy (luồng duy nhất)

GitHub Actions CI (build image per-service, tag SHA + alias develop/latest, push
Docker Hub `daotrongthuan`) → CD per-service (scp `k8s/<dir>/` lên VPS,
`kubectl apply`, rollout status, smoke). Chi tiết: `k8s/README.md`, `k8s/CI-CD.md`.

```bash
# Trên VPS — kiểm tra nhanh
kubectl -n ecommerce get pods            # kỳ vọng 14/14 Running + infra Running
curl -fsS http://162.4.177.91/actuator/health
```

Cửa ngõ duy nhất từ ngoài: **Traefik port 80 → api-gateway**. Không mở từng port
8081-8088 ra public.

## Frontend (`frontend/` — Vite + React + RTK Query + AntD)

SPA duy nhất gọi **1 origin**: `VITE_API_BASE_URL=http://162.4.177.91/api`
(Traefik :80 → api-gateway). `VITE_*` là **build-time** — đổi URL phải build
lại image với `--build-arg VITE_API_BASE_URL=...` (ConfigMap runtime không có
tác dụng). Docs hợp đồng: `docs/FE_API_CONTRACT.md` (path gateway, envelope
`ApiResponse`, doubled-prefix inventory/product/review), `docs/FE_AUTH_GATEWAY.md`
(login/refresh/guards), `docs/FE_SCAFFOLD.md` (cây thư mục, Dockerfile, k8s).

```bash
cd frontend && npm ci && npm run typecheck && npm run build
VITE_API_BASE_URL=http://162.4.177.91/api npm run dev   # http://localhost:5173
```

5 service rỗng (order/payment/notification/search/analytics) FE dùng mock
`src/mocks/` — không gọi API thật (BE trả 404). Deploy: CI job `frontend`
→ CD wave leaf `frontend-leaf` (sau review/analytics), image
`daotrongthuan/frontend:<sha>`, manifests `k8s/frontend/`.

## Địa chỉ kết nối (in-cluster DNS)

| Hạ tầng | Từ pod trong cluster | Từ ngoài (DEV public) |
|---|---|---|
| PostgreSQL | postgres:5432 | 162.4.177.91:5432 |
| MySQL | mysql:3306 | 162.4.177.91:3306 |
| Redis | redis:6379 | 162.4.177.91:6379 |
| Kafka | kafka:29092 | 162.4.177.91:9092 |
| Elasticsearch | elasticsearch:9200 | http://162.4.177.91:9200 |
| MinIO API | minio:9000 | http://162.4.177.91:9000 |
| MinIO Console | minio:9001 | http://162.4.177.91:9001 |
| Keycloak | keycloak:8080 | http://162.4.177.91:7080 |
| Zipkin | zipkin:9411 | http://162.4.177.91:9411 |

App services nối hạ tầng qua DNS nội bộ (`postgres:5432`, `kafka:29092`, ...).
Debug từ ngoài dùng LoadBalancer ports (cột phải) hoặc `kubectl port-forward`.
Kafka-UI (provectus) và redis-commander đã bỏ — dùng port-forward khi cần xem.

## Chạy project trong IntelliJ (dev local)

1. Mở `pom.xml` ở root dưới dạng Maven project, chọn JDK 21.
2. Hạ tầng dev local: trỏ tới VPS qua port-forward (ví dụ
   `kubectl port-forward svc/postgres 5432:5432 -n ecommerce`), hoặc dựng
   postgres/mysql/redis cục bộ riêng.
3. Chạy lớp `*Application` của service cần làm. `application.yml` mặc định trỏ
   tới localhost và mật khẩu mẫu tương ứng `.env.example`.
4. Spring Boot **không tự đọc** file `.env` — truyền biến môi trường vào Run
   Configuration khi cần.

Build ngoài Docker cần Java 21 và Maven 3.9+:

```bash
mvn -B package -DskipTests
mvn -pl services/order-service -am package -DskipTests
java -jar services/order-service/target/order-service-0.0.1-SNAPSHOT.jar
```

Build chỉ module `order-service` với `-am` cũng build parent. Không chạy
`spring-boot:run` với `-am` ở root vì parent là project `pom`.

## Maven + Nexus

Parent `pom.xml` khai báo repository `nexus-public` qua HTTP
(`http://162.4.177.91:18081/repository/maven-public/`). Maven 3.8.1+ chặn mặc định
mọi repo HTTP ngoài nên build hiện cảnh báo `Blocked mirror for repositories`
rồi fallback sang Maven Central (build vẫn SUCCESS nhưng chậm hơn và không dùng
Nexus cache). Nexus DEV không có HTTPS nên không đổi sang HTTPS được.

Copy template 1 lần cho dev local (KHÔNG dùng `.mvn/maven.config` vì CI tự tạo
`~/.m2/settings.xml` riêng):

```bash
mkdir -p ~/.m2 && cp maven-settings.xml ~/.m2/settings.xml
# neu ~/.m2/settings.xml da ton tai, gop phan <mirrors> nay vao
```

Verify (không còn dòng `Blocked mirror`):

```bash
mvn -q -DskipTests -pl services/product-service -am compile
```

## Gateway

Route ví dụ `/api/catalog/**` → catalog-service, `/api/order/**` → order-service.
`StripPrefix=2` loại `/api/catalog`, `/api/order` trước khi chuyển tiếp.

## Cấu trúc

- `k8s/`: manifests app (14 services) + `k8s/infra/` (postgres, mysql, redis,
  kafka, elasticsearch, minio, keycloak, zipkin, clamav, nexus).
- `Dockerfile`: build một Maven module bằng `SERVICE`, chạy JRE 21 bằng user không root.
- `services/*/pom.xml`: dependency theo nhu cầu từng project.
- `services/*/src/main/resources/application.yml`: kết nối host mặc định, k8s
  override bằng biến môi trường trong `k8s/*/configmap.yaml`.
- `infra/postgres`, `infra/mysql`, `infra/keycloak/realm.json`: single source of
  truth cho init ConfigMaps (`postgres-init`, `mysql-init`, `keycloak-realm`) —
  KHÔNG xóa, KHÔNG duplicate nội dung vào manifests.
- `infra/prometheus/`, `infra/grafana/`, `infra/loki/`, `infra/promtail/`:
  nguồn config observability để port lên k8s (Phase 4).
- `docs/OPERATIONS.md`: vận hành k8s, xử lý lỗi và giới hạn.
- `docs/VALIDATION.md`: checklist kiểm tra sau cắt compose.
- `services/inventory-service/docs/inventory-reliability-operations.md`: metrics,
  Kafka outbox/inbox, backup/restore và lịch load test Inventory.
- `performance/inventory/available-inventory.js`: k6 read-only load baseline;
  workflow chạy theo tuần.

Hạ tầng k8s dùng PVC `local-path` riêng để lưu dữ liệu. Kafka tắt tự tạo topic;
các topic sự kiện do job `kafka-init-topics` tạo.
