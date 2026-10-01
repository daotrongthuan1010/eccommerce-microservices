# Hướng dẫn vận hành quan sát (Grafana, Prometheus, Loki, Zipkin + hạ tầng)

> VPS: `162.4.177.91` — mọi port đã public `0.0.0.0` (hạ tầng học/dev). Tài khoản/mật khẩu lấy trong `.env` trên VPS.
> Quy trình sự cố chuẩn: **1. Phát hiện (Prometheus) → 2. Tìm nguyên nhân (Loki) → 3. Lỗi xuyên service (Zipkin) → 4. Kiểm tra nền (node-exporter)**. Chi tiết ở mục 9.
> Ngày cập nhật: 02/10/2026.

## 1. Bản đồ nhanh: cái gì ở đâu

| Công cụ | URL | Đăng nhập | Dùng để làm gì |
|---|---|---|---|
| Grafana | `http://162.4.177.91:3000` | user `GRAFANA_ADMIN_USER`, pass `GRAFANA_ADMIN_PASSWORD` trong `.env` | Dashboard + Explore logs (Loki) + metrics (Prometheus), đầu mối xử lý sự cố |
| Prometheus | `http://162.4.177.91:9090` | Không cần login | Truy vấn metrics thô (PromQL), kiểm tra target scrape |
| Loki | `http://162.4.177.91:3100` | Không cần (đi qua Grafana Explore) | Log tập trung toàn cụm (đọc từ Promtail) |
| Zipkin | `http://162.4.177.91:9411` | Không cần login | Xem trace request đi qua các service |
| Kafka-UI | `http://162.4.177.91:8090` | Không cần login | Xem cluster, topic, partition, message |
| Redis Commander | `http://162.4.177.91:8091` | Không cần login (đã nối sẵn pass trong `.env`) | Duyệt key Redis trên browser |
| Eureka | `http://162.4.177.91:8761` | Không cần login | Xem service nào đã đăng ký discovery |
| Keycloak | `http://162.4.177.91:7080` (admin console: `/admin`) | user `KEYCLOAK_ADMIN`, pass `KEYCLOAK_ADMIN_PASSWORD` | Quản lý realm `ecommerce`, user, role |
| MinIO Console | `http://162.4.177.91:9001` | user `MINIO_ROOT_USER`, pass `MINIO_ROOT_PASSWORD` | Xem bucket `product-assets`, upload/test file |
| Elasticsearch | `http://162.4.177.91:9200/_cluster/health` | Không cần login (dev, không auth) | Kiểm tra sức khỏe cluster tìm kiếm |
| ES exporter | `http://162.4.177.91:9114/metrics` | Không cần | Metrics ES cho Prometheus (job `infra-search`) |
| node-exporter | `http://162.4.177.91:9100/metrics` | Không cần | Metrics host VPS (job `infra-host`) |
| cAdvisor | `http://162.4.177.91:8089/metrics` | Không cần | Metrics container (job `infra-containers`, xem lưu ý mục 9.4) |
| API Gateway health | `http://162.4.177.91:8080/actuator/health` | Không cần | Kiểm tra gateway sống không |

## 2. Prometheus (`:9090`)

Prometheus đi **kéo (pull)** metrics từ các service theo lịch trong `infra/prometheus/prometheus.yml` (`scrape_interval: 15s`).

### 2.1. Kiểm tra target có scrape được không

1. Mở `http://162.4.177.91:9090/targets` — chuẩn là **14/14 UP**:
   - `spring-apps` (10 service Java qua `/actuator/prometheus`): discovery `:8761`, gateway `:8080`, auth `:8081` … search `:8088`.
   - `infra-host`: `node-exporter:9100`. `infra-containers`: `cadvisor:8080`. `infra-search`: `elasticsearch-exporter:9114`. Cộng chính prometheus.
2. Cột State phải là **UP** (xanh). Nếu DOWN: service chưa chạy hoặc sai hostname/port trong `prometheus.yml`.
3. Thêm service mới: thêm target vào `infra/prometheus/prometheus.yml` rồi `docker compose up -d prometheus` (service phải expose `management.endpoints.web.exposure.include: health,info,prometheus`).

### 2.2. Truy vấn PromQL mẫu (tab Graph / Explore)

Mở `http://162.4.177.91:9090/query` và thử:

```
# Bước 1 — PHÁT HIỆN: 5xx có đang vọt không, ở endpoint nào (dùng khi sự cố, xem mục 9.1)
sum(rate(http_server_requests_seconds_count{job="spring-apps",status=~"5.."}[5m])) by (instance, uri)

# Thu hẹp 1 service, ví dụ auth-service
sum(rate(http_server_requests_seconds_count{instance="auth-service:8081",status=~"5.."}[5m])) by (uri)

# Service nào đang UP (1 = lên, 0 = rớt)
up{job="spring-apps"}

# Thời gian đáp ứng p95 của API login
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri="/auth/login"}[5m])) by (le))

# JVM heap đang dùng theo từng service
jvm_memory_used_bytes{area="heap"}

# Kết nối HikariCP đang mở
hikaricp_connections_active

# Bước 4 — KIỂM TRA NỀN: CPU/RAM host (xem mục 9.4)
1 - avg(rate(node_cpu_seconds_total{job="infra-host",mode="idle"}[5m]))
1 - (node_memory_MemAvailable_bytes{job="infra-host"} / node_memory_MemTotal_bytes{job="infra-host"})

# Sức khỏe Elasticsearch qua exporter
elasticsearch_cluster_health_status{color="green"}
```

Bấm **Execute**, chuyển tab **Graph** để xem đường theo thời gian.

## 3. Grafana (`:3000`)

Grafana đã được provision sẵn 2 datasource (`infra/grafana/provisioning/datasources/datasources.yml`):
Prometheus (`http://prometheus:9090`, default) + Loki (`http://loki:3100`).
Dashboard **Spring Boot Overview** (folder "Spring Boot") cũng đã provision sẵn
(`infra/grafana/provisioning/dashboards/`): Targets UP, HTTP req/s gateway,
HTTP p95, HTTP 5xx, JVM heap, HikariCP, Host CPU/RAM, JVM threads, ES health.

### 3.1. Đăng nhập lần đầu

1. Mở `http://162.4.177.91:3000` → login bằng `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` trong `.env`.
2. Vào **Connections → Data sources** → thấy `Prometheus` (default) và `Loki`, bấm **Save & test** phải báo thành công cả 2.
3. Vào **Dashboards** → mở **Spring Boot Overview** là thấy toàn cụm (không cần tạo tay).

## 3a. Loki + Promtail — log tập trung (`:3100`)

Log mọi container Docker được Promtail đọc từ `/var/lib/docker/containers/*/*.log`,
parse JSON của docker (`log/stream/time`) rồi push về Loki (`http://loki:3100`,
retention 7 ngày, config `infra/loki/loki.yml`). Xem log trong Grafana:

1. Menu trái **Explore** → chọn datasource **Loki**.
2. Query toàn cụm: `{job="docker"}` → Run, chọn khoảng thời gian góc phải.
3. **Lọc auth-service** (label hiện chỉ có `filename` là ID container + `job="docker"`,
   nên lọc theo nội dung log — muốn label `container=auth-service` gọn hơn thì nâng
   promtail lên `docker_sd_configs`, xem ghi chú cuối mục 9.2):
   ```
   {job="docker"} |~ "auth-service"
   ```
4. **Chỉ xem lỗi:**
   ```
   {job="docker"} |~ "auth-service" |~ "ERROR|Exception"
   ```
5. Kiểm tra Loki sống: `http://162.4.177.91:3100/ready` phải trả `ready`;
   `http://162.4.177.91:3100/loki/api/v1/labels` phải có `job`, `filename`.
6. Kiểm tra Promtail có tail đủ container: log promtail phải có dòng
   `tail routine: started` cho từng `*-json.log`:
   ```bash
   docker logs eccommerce-promtail-1 2>&1 | grep -c "tail routine: started"
   ```

### 3.2. Tạo dashboard thủ công (chỉ khi cần thêm panel ngoài dashboard provision sẵn)

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

## 9. Quy trình xử lý sự cố 4 bước (làm theo thứ tự)

> Ví dụ xuyên suốt: **auth-service trả error 500**. Mỗi bước ghi rõ mở cái gì, bấm gì, đọc gì.

### 9.1. Bước 1 — PHÁT HIỆN: Grafana + Prometheus (5xx vọt ở đâu, từ khi nào)

1. Mở dashboard **Spring Boot Overview** → panel **HTTP 5xx rate**: thấy đường của
   `auth-service:8081` vọt lên khỏi 0 → có 500. Ghi lại **thời điểm bắt đầu vọt**
   (ví dụ 14:05) để dùng ở bước 2.
2. Muốn biết chính xác endpoint: Explore → datasource Prometheus → query:
   ```
   sum(rate(http_server_requests_seconds_count{instance="auth-service:8081",status=~"5.."}[5m])) by (uri)
   ```
   Kết quả `uri="/auth/login" value>0` → lỗi ở login. Đổi time range góc phải về
   đúng khung giờ vọt để loại nhiễu.
3. Nếu panel 5xx phẳng mà user vẫn báo lỗi: kiểm tra panel **Targets UP**
   (`sum(up{job="spring-apps"})` phải = 10) — có thể service rớt hẳn (không sinh 5xx
   mà sinh `up=0`), hoặc lỗi 4xx (sai token/403 — xem panel **HTTP req/s** theo status).

### 9.2. Bước 2 — TÌM NGUYÊN NHÂN: Grafana + Loki (đọc stack trace đúng khung giờ)

1. Grafana → **Explore** → datasource **Loki**.
2. Ô query gõ đúng khung giờ ở bước 1 (time picker, ví dụ Last 15 minutes quanh 14:05):
   ```
   {job="docker"} |~ "auth-service" |~ "ERROR|Exception"
   ```
3. Đọc từ dòng ERROR đầu tiên **ngược lên trên**: thường thấy
   `NullPointerException`, `Connection refused` (mất DB/Redis/Keycloak),
   `I/O error on GET ... /certs` (sai JWK URI — bệnh từng gặp ở user-service),
   hoặc `401` hàng loạt (hết hạn key). Dòng log Spring ghi đủ `class:line`
   để mở code sửa.
4. Muốn xem toàn bộ request lỗi đó (không chỉ dòng ERROR): bỏ filter thứ 2,
   chỉ giữ `{job="docker"} |~ "auth-service"`, kéo time range hẹp ±2 phút quanh lỗi.
5. Ghi chú label: hiện Loki chỉ có label `job="docker"` + `filename` (ID container),
   lọc service bằng regex nội dung `[auth-service]`. Khi nào thấy bất tiện thì nâng
   `infra/promtail/promtail.yml` sang `docker_sd_configs` để có label
   `container=eccommerce-auth-service-1`, query gọn `{container="eccommerce-auth-service-1"}`.

### 9.3. Bước 3 — LỖI XUYÊN SERVICE: Zipkin (request kẹt ở khâu nào)

Dùng khi bước 2 chỉ thấy `timeout` / `I/O error` mà không rõ lỗi ở service mình hay service下游:

1. Gọi lại API lỗi vài lần để sinh trace mới (sampling đang `1.0` = lấy 100%).
2. Mở `http://162.4.177.91:9411` → **Run Query** (service = all) → click trace mới nhất
   có màu đỏ / duration dài.
3. Đọc cây span từ trái sang: ví dụ `api-gateway (3.0s) → auth-service (2.9s) → keycloak (?)`:
   span nào chiếm gần hết thời gian cha thì nghẽn ở đó. `http.status_code=500` trên span
   nào thì lỗi phát sinh ở service đó.
4. Lọc nhanh: **Service Name** = `auth-service`, **Min Duration** = `500ms`,
   **Tags** = `error=true`.

### 9.4. Bước 4 — KIỂM TRA NỀN: Grafana + node-exporter (có phải hết tài nguyên)

Dùng khi nhiều service cùng lỗi một lúc, hoặc log có `OutOfMemoryError` / timeout hàng loạt:

1. Dashboard **Spring Boot Overview** → panel **Host CPU / RAM** (job `infra-host`):
   CPU ~1.0 hoặc RAM > 90% kéo dài → VPS nghẽn, xem tiếp service nào ăn nhiều ở panel
   **JVM heap** / **JVM threads live**.
2. Kiểm tra trực tiếp khi nghi exporter sai:
   `http://162.4.177.91:9100/metrics` (node-exporter), `:9114/metrics` (ES),
   `:8089/metrics` (cAdvisor) phải trả 200.
3. Hạn chế đã biết: VPS dùng Docker + containerd backend nên cAdvisor **không thấy tên
   container riêng lẻ** (log `failed to identify the read-write layer`), chỉ có metrics
   cgroup hệ thống. Muốn RAM từng container: `docker stats` trên VPS, hoặc bật Docker
   metrics endpoint khi cần.
4. ES: panel **ES cluster health** phải = 1 (green). Rớt thì kiểm tra
   `http://162.4.177.91:9200/_cluster/health` trực tiếp.

## 10. Xử lý sự cố thường gặp (hạ tầng quan sát)

| Triệu chứng | Kiểm tra |
|---|---|
| Không mở được URL nào | `ss -tln` trên VPS phải thấy `0.0.0.0:<port>`; container `Up` (`docker compose --profile apps ps`) |
| Prometheus target DOWN | Service backend chưa chạy; sai port trong `infra/prometheus/prometheus.yml`; `up -d prometheus` lại sau khi sửa |
| Grafana `Save & test` fail | Prometheus container rớt; datasource url phải là `http://prometheus:9090` (tên service nội bộ, không phải IP VPS) |
| Zipkin trống | Chưa có traffic — gọi API qua gateway trước; kiểm tra `ZIPKIN_ENDPOINT` trong service |
| Kafka-UI báo không nối cluster | Kafka container chưa healthy; `KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS` phải là `kafka:29092` |
| Redis Commander trắng key | Bình thường khi chưa có code ghi Redis; test bằng `redis-cli` ghi key thử |
| Keycloak redirect sai host sau login console | Dev đã tắt strict (`KC_HOSTNAME_STRICT=false`); prod cần set `KC_HOSTNAME` thật |
| Loki query không ra log mới | Promtail rớt hoặc Loki chưa ready (`/ready` phải trả `ready`); kiểm tra `docker logs eccommerce-promtail-1` có `tail routine: started` |
| Grafana Explore Loki báo datasource không tồn tại | Grafana chưa load `datasources.yml` mới — `docker restart eccommerce-grafana-1`; kiểm tra `/api/datasources` có `Loki` |
| Panel Host CPU/RAM trống | node-exporter DOWN (`:9100/metrics` phải 200); job `infra-host` trong `prometheus.yml` |
