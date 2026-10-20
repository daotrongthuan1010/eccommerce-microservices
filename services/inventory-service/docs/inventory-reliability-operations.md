# Inventory Service: reliability và vận hành

## Phạm vi và giả định

Tài liệu này áp dụng cho Inventory Service, PostgreSQL `inventory_db`, Redis cache và Kafka. Hibernate `ddl-auto:update` tạo/cập nhật bảng `inventory_event_outbox` và `inventory_event_inbox`; dự án không dùng Flyway. Database production cần sao lưu ngoài host ứng dụng và phải kiểm thử restore định kỳ.

## Observability

- Prometheus scrape `/actuator/prometheus`; Grafana có dashboard `Inventory Service Operations`; Zipkin nhận trace.
- Actuator health probes là `/actuator/health/liveness` và `/actuator/health/readiness`. Endpoint actuator hiện được common-security permit; giới hạn truy cập bằng firewall/network policy để chỉ Prometheus và vận hành tin cậy truy cập.
- Log correlation gồm tên service, `traceId`, `spanId`. Mặc định lấy mẫu trace 10%; đặt `TRACING_SAMPLING_PROBABILITY` theo môi trường và capacity.
- Metric chính: HTTP latency/error, stock movement count, event outbox backlog/age/publish failure, Product event duplicate/stale/failure/DLT và Hikari connection wait.
- Prometheus rules phát cảnh báo trong giao diện Alerts. Để gửi notification, nối Prometheus với Alertmanager theo quy trình vận hành môi trường.
- Không dùng event ID, SKU, user ID hoặc inventory ID làm label metric; các giá trị đó chỉ xuất hiện trong log có kiểm soát để tránh cardinality tăng vô hạn.

## Outbox phát Inventory movement

1. API cập nhật bucket tồn và ghi `stock_movement` bằng JDBC.
2. Cùng transaction, repository ghi envelope `InventoryMovementRecorded.v1` vào `inventory_event_outbox`; lỗi ghi outbox sẽ rollback movement và số dư.
3. Poller claim tối đa 100 event bằng `FOR UPDATE SKIP LOCKED`, đặt lease ba phút rồi commit transaction claim. Lease dài hơn `delivery.timeout.ms` 120 giây của Kafka producer để giảm khả năng instance khác nhận lease trong lúc gửi cũ còn đang chờ.
4. Kafka publish chạy sau khi transaction đã commit. Claim chỉ chọn event pending sớm nhất theo từng Inventory aggregate; broker ack thì lưu `published_at`, lỗi gửi được retry với backoff tối đa 300 giây.
5. Payload chứa `eventId`, `eventType`, `occurredAt`, Inventory bucket và snapshot movement. Nếu tiến trình chết sau khi Kafka nhận event nhưng trước khi lưu `published_at`, lease hết hạn và message có thể phát lại. Consumer downstream phải khử trùng lặp theo `eventId`.
6. Event đã publish được giữ 30 ngày; event lỗi không tự xóa. Theo dõi tuổi backlog và xác minh topic/broker khi backlog tăng.

Tạo topic trên môi trường Compose bằng `bash ./infra/scripts/create-inventory-kafka-topics.sh` trước khi chạy Inventory Service. Mặc định dev dùng replication factor 1; môi trường nhiều broker phải đặt replication factor phù hợp. `product-events.DLT` cần cùng số partition với `product-events` để giữ partition tương ứng.

## Inbox và event trùng

Product consumer yêu cầu producer gửi `eventId` ổn định, `eventType`, `skuCode` và tăng `version` trên từng SKU. Unique key `(consumer_name, event_id)` và hash payload được lưu atomically cùng cập nhật projection.

- Cùng `eventId` và payload: tính duplicate, không ghi lại projection.
- Cùng `eventId` nhưng payload khác: báo conflict và đưa record sang DLT để điều tra producer.
- Event có version cũ hoặc bằng version projection: ghi nhận stale, không lùi trạng thái SKU.
- Lỗi xử lý được retry 3 lần, cách nhau 1 giây; sau đó gửi tới `product-events.DLT` và commit offset nếu publish DLT thành công.
- Inbox giữ 90 ngày. Retention phải dài hơn Kafka retention và cửa sổ replay dự kiến. Khi đổi retention, đánh giá lại khả năng phát lại event cũ.
- Record DLT chỉ replay sau khi xác định nguyên nhân và bảo đảm inbox/version có thể xử lý an toàn.

## Disaster recovery

### Backup

Script `infra/scripts/backup-inventory-db.sh` tạo PostgreSQL custom dump, xác minh archive, mã hóa AES-256 bằng GPG và ghi SHA-256 manifest. Script yêu cầu file passphrase `INVENTORY_BACKUP_KEY_FILE` nằm ngoài repository. Chạy mỗi 4 giờ trên host có Docker Compose; đồng bộ file `.dump.gpg` và manifest sang object storage khác vùng với encryption-at-rest, versioning và retention lock. Không chỉ lưu backup trên cùng volume/host PostgreSQL.

Ví dụ cron trên Linux (đặt secrets trong file môi trường riêng có quyền `0600`):

```cron
0 */4 * * * cd /opt/eccommerce-microservices && set -a && . /etc/inventory-backup.env && set +a && bash ./infra/scripts/backup-inventory-db.sh >> /var/log/inventory-backup.log 2>&1
```

Logical dump cho điểm khôi phục tại thời điểm chạy. Nếu yêu cầu RPO ngắn hơn chu kỳ backup, cấu hình WAL archiving/PITR và giữ WAL ở storage độc lập; cấu hình đó phụ thuộc hạ tầng PostgreSQL production và chưa được bật bởi Compose dev.

### Restore

1. Chọn backup cùng manifest từ storage độc lập; xác minh checksum và quyền truy cập khóa GPG.
2. Khôi phục vào database cô lập trước, không ghi đè production trong diễn tập.
   Tạo database đích rỗng trước khi restore, ví dụ: `docker compose exec -T postgres createdb -U postgres inventory_restore`.
3. Lệnh yêu cầu xác nhận đích tường minh:

```bash
INVENTORY_BACKUP_KEY_FILE=/run/secrets/inventory-backup-passphrase \
INVENTORY_RESTORE_CONFIRM=inventory_restore \
bash ./infra/scripts/restore-inventory-db.sh /secure-backups/inventory.dump.gpg inventory_restore
```

4. Kiểm tra schema và FK, đối chiếu số dòng `inventory`, `stock_movement`, receipt/transfer, chạy smoke API read-only, kiểm tra outbox/inbox và audit.
5. Chỉ chuyển traffic sau khi owner dữ liệu duyệt kết quả restore; ghi lại thời gian restore và data loss đo được.

Mục tiêu vận hành ban đầu để diễn tập và đo lại: RPO tối đa 4 giờ với backup mỗi 4 giờ; RTO tối đa 2 giờ cho restore từ dump. Đây là mục tiêu cần được xác nhận bằng diễn tập trên hạ tầng production, không phải cam kết từ script.

Thực hiện restore drill tối thiểu mỗi quý vào database cô lập. Theo dõi backup age, checksum, dung lượng, thời gian dump/restore và quyền truy cập khóa. Thử nghiệm khôi phục cả credential và topic/config cần thiết, không chỉ dữ liệu PostgreSQL.

## Load test định kỳ

`performance/inventory/available-inventory.js` chạy GET read-only `/inventory/available`, lấy token mới bằng OAuth client credentials, tăng dần lên tối đa 20 VU, chạy tải ổn định 2 phút và hạ tải. Ngưỡng ban đầu: lỗi dưới 1%, p95 dưới 750 ms, p99 dưới 1.5 giây. Đặt URL staging ở secret `INVENTORY_STAGING_BASE_URL` và thông tin OAuth test client trong repository secrets; có thể đặt `INVENTORY_READ_PATH`, `AUTH_CLIENT_SCOPE`, `INVENTORY_K6_PEAK_VUS` trong repository variables. Script chặn mức tải cấu hình cao hơn 20 VU để tránh tăng tải ngoài dự kiến.

Workflow `.github/workflows/inventory-weekly-load.yml` chạy thứ Hai lúc 03:00 UTC và cho phép chạy thủ công. Nếu thiếu secrets, workflow cảnh báo và bỏ qua. Chỉ khai báo URL môi trường staging vào `INVENTORY_STAGING_BASE_URL`; endpoint cần truy cập được từ GitHub runner. Nếu staging chỉ nằm trong private network, đổi runner thành self-hosted trong mạng staging. Dùng test client chỉ có role USER và dữ liệu staging ổn định.

## Checklist sau deploy

- Prometheus target `inventory-service:8084` đang UP; dashboard có HTTP, database pool, outbox và consumer metrics.
- `inventory_event_outbox_pending` và `inventory_cache_invalidation_pending` không tăng liên tục; oldest age dưới ngưỡng.
- Kafka có đủ ba topic, consumer đang xử lý; DLT được xem xét mỗi ngày có event mới.
- Backup encrypted gần nhất có manifest hợp lệ và đã được copy offsite.
- Weekly load run có artifact kết quả; theo dõi p95/p99 và so sánh xu hướng theo release.
