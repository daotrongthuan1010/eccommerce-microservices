# Hướng dẫn dùng Grafana, Prometheus, Zipkin, Kafka-UI, Redis Commander và các thành phần quan sát

> VPS: `162.4.177.91` — mọi port đã public `0.0.0.0` (hạ tầng học/dev). Tài khoản/mật khẩu lấy trong `.env` trên VPS.
> Ngày cập nhật: 01/10/2026.

## 1. Bản đồ nhanh: cái gì ở đâu

| Công cụ | URL | Đăng nhập | Dùng để làm gì |
|---|---|---|---|
| Grafana | `http://162.4.177.91:3000` | user `GRAFANA_ADMIN_USER`, pass `GRAFANA_ADMIN_PASSWORD` trong `.env` | Dashboard, vẽ biểu đồ metrics |
| Prometheus | `http://162.4.177.91:9090` | Không cần login | Truy vấn metrics thô (PromQL), kiểm tra target scrape |
| Zipkin | `http://162.4.177.91:9411` | Không cần login | Xem trace request đi qua các service |
| Kafka-UI | `http://162.4.177.91:8090` | Không cần login | Xem cluster, topic, partition, message |
| Redis Commander | `http://162.4.177.91:8091` | Không cần login (đã nối sẵn pass trong `.env`) | Duyệt key Redis trên browser |
| Eureka | `http://162.4.177.91:8761` | Không cần login | Xem service nào đã đăng ký discovery |
| Keycloak | `http://162.4.177.91:7080` (admin console: `/admin`) | user `KEYCLOAK_ADMIN`, pass `KEYCLOAK_ADMIN_PASSWORD` | Quản lý realm `ecommerce`, user, role |
| MinIO Console | `http://162.4.177.91:9001` | user `MINIO_ROOT_USER`, pass `MINIO_ROOT_PASSWORD` | Xem bucket `product-assets`, upload/test file |
| Elasticsearch | `http://162.4.177.91:9200/_cluster/health` | Không cần login (dev, không auth) | Kiểm tra sức khỏe cluster tìm kiếm |
| API Gateway health | `http://162.4.177.91:8080/actuator/health` | Không cần | Kiểm tra gateway sống không |

## 2. Prometheus (`:9090`)

Prometheus đi **kéo (pull)** metrics từ các service theo lịch trong `infra/prometheus/prometheus.yml` (`scrape_interval: 15s`).

### 2.1. Kiểm tra target có scrape được không

1. Mở `http://162.4.177.91:9090/targets`.
2. Cột State phải là **UP** (xanh). Nếu DOWN: service chưa chạy hoặc sai hostname/port trong `prometheus.yml`.
3. Job hiện có:
   - `prometheus` → chính nó (`localhost:9090`).
   - `spring-apps` → `discovery-service:8761`, `api-gateway:8080`, `auth-service:8081`, `user-service:8082` qua đường `/actuator/prometheus`.

> Lưu ý: hiện mới scrape 4 service Java. Muốn thêm catalog (8083), inventory (8084)... thì thêm target vào `infra/prometheus/prometheus.yml` rồi `docker compose up -d prometheus`. Các service đã expose sẵn (`management.endpoints.web.exposure.include: health,info,prometheus` trong mọi `application.yml`).

### 2.2. Truy vấn PromQL mẫu (tab Graph / Explore)

Mở `http://162.4.177.91:9090/query` và thử:

```
# Service nào đang UP (1 = lên, 0 = rớt)
up{job="spring-apps"}

# Tỉ lệ lỗi HTTP 5xx trên gateway (cần có traffic)
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))

# Thời gian đáp ứng p95 của API login
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri="/login"}[5m])) by (le))

# JVM heap đang dùng theo từng service
jvm_memory_used_bytes{area="heap"}

# Kết nối HikariCP đang mở
hikaricp_connections_active
```

Bấm **Execute**, chuyển tab **Graph** để xem đường theo thời gian.

## 3. Grafana (`:3000`)

Grafana đã được provision sẵn datasource Prometheus (`infra/grafana/provisioning/datasources/prometheus.yml`, url `http://prometheus:9090`, là default).

### 3.1. Đăng nhập lần đầu

1. Mở `http://162.4.177.91:3000` → login bằng `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` trong `.env`.
2. Vào **Connections → Data sources** → thấy `Prometheus` (default) là OK, bấm **Save & test** phải báo thành công.

### 3.2. Tạo dashboard đầu tiên (5 phút)

1. Menu trái **Dashboards → New → New dashboard → Add visualization** → chọn datasource **Prometheus**.
2. Ô Metric explorer gõ `up{job="spring-apps"}` → **Run queries** → đổi Panel title thành `Service UP`.
3. **Add** thêm panel: query `jvm_memory_used_bytes{area="heap"}` → title `JVM heap`.
4. Thêm panel Stat: query `hikaricp_connections_active` → title `DB connections`.
5. Bấm **Save dashboard** (icon đĩa), đặt tên `Ecommerce overview`.

### 3.3. Xem log lỗi metric không có dữ liệu

- Panel hiện `No data`: kiểm tra khoảng thời gian góc phải (chọn Last 1 hour), và kiểm tra target trong Prometheus (mục 2.1).
- Các metric `http_server_requests_*` chỉ xuất hiện **sau khi có request thật** — gọi vài API qua gateway (xem `docs/INFRASTRUCTURE.md` mục 3.3) rồi quay lại.

## 4. Zipkin (`:9411`)

Mọi service Java đã cấu hình gửi span về `http://zipkin:9411/api/v2/spans` (`management.zipkin.tracing.endpoint` trong `application.yml`, sampling `probability: 1.0` = lấy 100% trace, hợp cho học).

### 4.1. Xem trace

1. Gọi vài API qua gateway để sinh traffic, ví dụ:
   ```bash
   curl http://162.4.177.91:8080/actuator/health
   curl -X POST http://162.4.177.91:8080/api/auth/login \
     -H 'Content-Type: application/json' -d '{"username":"admin","password":"Admin123!"}'
   ```
2. Mở `http://162.4.177.91:9411` → bấm **Run Query** (giữ nguyên service = all).
3. Click 1 trace → thấy các span: `api-gateway` → `auth-service`, mỗi span có thời gian, tag `http.status_code`, lỗi (màu đỏ) nếu có.

### 4.2. Lọc hay dùng

- Ô **Service Name**: chọn `api-gateway` để chỉ xem trace qua gateway.
- **Min Duration**: gõ `500ms` để tìm request chậm.
- **Tags**: `error=true` để tìm trace lỗi.

> Giới hạn hiện tại: trace mới có ý nghĩa khi đã viết controller nghiệp vụ gọi liên service. Ở giai đoạn scaffold, trace chủ yếu là health-check và login.

## 5. Kafka-UI (`:8090`)

Đã nối sẵn vào Kafka nội bộ (`KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092`, tên cluster `local`).

### 5.1. Xem topic

1. Mở `http://162.4.177.91:8090` → click cluster **local** → **Topics**.
2. Hai topic seed sẵn (tạo bởi job `kafka-init` trong compose): `ecommerce.auth.events`, `ecommerce.user.events` (3 partitions, replication 1).
3. Chưa có message là bình thường — producer/consumer chưa viết (scaffold chưa có code nghiệp vụ).

### 5.2. Tạo topic thử + gửi message thử (học)

1. **Topics → Create Topic**: name `ecommerce.test.events`, partitions `3`, replication factor `1` → Create.
2. Vào topic vừa tạo → **Messages → Produce Message**: key `demo-1`, content `{"hello":"kafka"}` → Send.
3. Sang tab **Messages** → bấm refresh sẽ thấy message vừa gửi (partition, offset, timestamp).
4. Muốn dev local nối Kafka VPS trực tiếp: bootstrap `162.4.177.91:9092` (`.env` trên VPS phải có `KAFKA_EXTERNAL_HOST=162.4.177.91`).

### 5.3. Xem consumer group

Menu **Consumer Groups**: khi nào viết consumer (group-id `auth-service`... trong `application.yml`) thì vào đây xem lag — lag tăng liên tục nghĩa là consumer xử lý không kịp.

## 6. Redis Commander (`:8091`)

Đã nối sẵn (`REDIS_HOSTS: local:redis:6379:0:<pass>`), vào là thấy DB 0.

### 6.1. Duyệt key

1. Mở `http://162.4.177.91:8091` → cây thư mục bên trái là các key (token login, cache... khi đã viết code dùng Redis).
2. Click key → xem type (string/hash...), value, TTL.
3. Ô search trên cùng: gõ pattern `spring:*` hoặc `*token*` để lọc.

### 6.2. Thao tác hay dùng khi học

- Xem TTL còn lại của key token để hiểu cơ chế hết hạn.
- **Xoá key** (nút delete) để giả lập mất cache/token rồi gọi lại API xem hành vi.
- Không dùng nút **Flush** trên môi trường có dữ liệu — nó xoá toàn bộ DB.

## 7. Các thành phần còn lại

### 7.1. Eureka (`:8761`)

Mở `http://162.4.177.91:8761` → mục **Instances currently registered**: phải thấy `API-GATEWAY`, `AUTH-SERVICE`, `USER-SERVICE`... (tên in hoa). Service nào vắng mặt = chưa đăng ký được (kiểm tra `EUREKA_SERVER_URL` và log service đó). Đây là chỗ tra cứu nhanh nhất khi gateway báo 404 do không tìm thấy service backend.

### 7.2. Keycloak (`:7080`)

1. Admin console: `http://162.4.177.91:7080/admin` → login admin trong `.env`.
2. Chọn realm **ecommerce** (đã import từ `infra/keycloak/realm.json`): xem **Users** (`admin/Admin123!` role ADMIN+USER, `student/Student123!` role USER), **Realm roles** (ADMIN, USER), **Clients** (`ecommerce-app`, public client, bật direct access grant nên `curl` login được).
3. Kiểm tra nhanh realm sống: `http://162.4.177.91:7080/realms/ecommerce/.well-known/openid-configuration` phải trả JSON.

### 7.3. MinIO Console (`:9001`) + API (`:9000`)

1. Console `http://162.4.177.91:9001` → login root trong `.env` → bucket **product-assets** (job `minio-init` tự tạo). Bucket không bật public access.
2. Upload thử 1 file qua Console để học luồng, sau này catalog-service sẽ dùng SDK với cùng access-key/bucket (`storage.minio.*` trong `catalog-service` config, endpoint nội bộ `http://minio:9000`).

### 7.4. Elasticsearch (`:9200`)

```bash
curl http://162.4.177.91:9200/_cluster/health
# {"cluster_name":"docker-cluster","status":"green|yellow",...}
```
Chưa có index/mapping (scaffold chưa đồng bộ catalog → ES). Khi search-service tạo index, xem danh sách index: `curl http://162.4.177.91:9200/_cat/indices?v`.

## 8. Kịch bản học gợi ý (đi hết 5 công cụ trong 15 phút)

1. **Eureka**: mở `:8761`, ghi lại các service đang UP.
2. **Login qua gateway** (sinh traffic + token Redis + event Kafka tương lai):
   ```bash
   curl -s -X POST http://162.4.177.91:8080/api/auth/login \
     -H 'Content-Type: application/json' -d '{"username":"admin","password":"Admin123!"}'
   ```
3. **Zipkin** (`:9411` → Run Query): tìm trace login vừa gọi, xem span gateway → auth.
4. **Redis Commander** (`:8091`): tìm key token vừa sinh, xem TTL.
5. **Prometheus** (`:9090/query`): chạy `up{job="spring-apps"}` và `http_server_requests_seconds_count` — thấy counter request tăng.
6. **Grafana** (`:3000`): mở dashboard đã tạo ở mục 3.2, thấy số liệu vừa sinh.
7. **Kafka-UI** (`:8090`): produce 1 message thử vào topic test, đọc lại ở tab Messages.

## 9. Xử lý sự cố thường gặp

| Triệu chứng | Kiểm tra |
|---|---|
| Không mở được URL nào | `ss -tln` trên VPS phải thấy `0.0.0.0:<port>`; container `Up` (`docker compose --profile apps ps`) |
| Prometheus target DOWN | Service backend chưa chạy; sai port trong `infra/prometheus/prometheus.yml`; `up -d prometheus` lại sau khi sửa |
| Grafana `Save & test` fail | Prometheus container rớt; datasource url phải là `http://prometheus:9090` (tên service nội bộ, không phải IP VPS) |
| Zipkin trống | Chưa có traffic — gọi API qua gateway trước; kiểm tra `ZIPKIN_ENDPOINT` trong service |
| Kafka-UI báo không nối cluster | Kafka container chưa healthy; `KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS` phải là `kafka:29092` |
| Redis Commander trắng key | Bình thường khi chưa có code ghi Redis; test bằng `redis-cli` ghi key thử |
| Keycloak redirect sai host sau login console | Dev đã tắt strict (`KC_HOSTNAME_STRICT=false`); prod cần set `KC_HOSTNAME` thật |
