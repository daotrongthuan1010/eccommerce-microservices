# Notification-service: cách chạy, test và rollback

## Tổng quan

Notification-service nhận event `OrderConfirmed`/`OrderCanceled` từ Kafka topic
`order-events`, tạo `NotificationRequest` bền vững (businessKey chống trùng), rồi
worker nền gửi mail qua **Mailpit** (SMTP :1025). Mailpit là mail server giả lập
open-source nhẹ (Go, ~30MB RAM) — nhận được hàng trăm nghìn mail demo, có UI :8025.

- Docs nguồn: `docs/Ecommerce_Microservices_AgentAI/reference/chapters/15-notification-service-gia-lap-sms-va-gia-lap-mail.md`
- Hợp đồng Kafka: `docs/Ecommerce_Microservices_AgentAI/shared/contracts/03-kafka-events.md`
  (order → notification: `OrderConfirmed / OrderCanceled`)

## Kiến trúc slice đã implement

```
Kafka order-events ──> OrderEventConsumer (inbox dedup theo eventId+source)
                          │  chot templateVersion ACTIVE moi nhat + bien (orderCode, totalAmount)
                          │  KHONG render, KHONG gui — chi tao NotificationRequest QUEUED
                          │  template ACTIVE khong ton tai -> DLQ TEMPLATE_MISSING
                          ▼
                    NotificationWorker (poll 2s, batch 100 LIMIT DB, 8 threads)
                          ▼
                    NotificationRequestProcessor (REQUIRES_NEW, lock row)
                          │
                          ├─ Giai doan 0: kenh ho tro? (SMS -> DLQ UNSUPPORTED_CHANNEL)
                          ├─ Giai doan 1: RESOLVE userId qua Keycloak Admin API (cache 10')
                          │    Found / UserNotFound (permanent) / NoEmail (permanent) / Unavailable (retry)
                          ├─ Giai doan 2: RENDER template version DA CHOT + bien da chot
                          │    bien thieu -> DLQ TEMPLATE_VARIABLES_MISSING (khong gui noi dung sai)
                          │    version mat  -> DLQ TEMPLATE_VERSION_MISSING
                          └─ Giai doan 3: SEND SMTP Mailpit (header X-Notification-Business-Key)
                               ACCEPTED / FAILED_RETRYABLE (backoff+jitter, max 5)
                               UNKNOWN -> tra cuu Mailpit API theo businessKey truoc retry (NOTI-07)
                               vu han  -> FAILED_FINAL + DLQ RETRY_EXHAUSTED
```

**Template (NOTI-02)**: bang `notification_template` (key + version + channel + locale + status
DRAFT/ACTIVE/RETIRED + required_variables). Seed V2: `order-confirmed` v1, `order-canceled` v1
(tiếng Việt, biến `orderCode, displayName, totalAmount`). Request chốt `template_version` lúc tạo —
sửa template = tạo version mới, request cũ render đúng version đã chốt (NOTI-UC04 bước 5).

**Snapshot người nhận (docs 15.2)**: `recipient_email`, `recipient_name` lưu trên request sau khi
resolve (displayName = firstName + lastName từ Keycloak); API list KHÔNG trả email/subject/body
(che PII).

## Biến môi trường

| Biến | Mặc định local | Ý nghĩa |
|---|---|---|
| `MAILPIT_API_BASE` | `http://localhost:8025` | Mailpit HTTP API (tra cứu UNKNOWN) |
| `MAILPIT_SMTP_HOST/PORT` | `localhost:1025` | SMTP gửi mail |
| `KEYCLOAK_ADMIN_CLIENT_ID/SECRET` | trống | Service account Keycloak (view-users) để resolve email. Trống = mail retryable với lý do `RECIPIENT_UNRESOLVED` |
| `ORDER_EVENTS_TOPIC` | `order-events` | Topic consume |
| `NOTIFICATION_FROM` | `shop@shop-hatd.online` | From address |

## Cách chạy local (dev)

1. Mailpit: `docker run -d -p 8025:8025 -p 1025:1025 axllent/mailpit`
2. `mvn -pl services/notification-service spring-boot:run`
3. UI Mailpit: http://localhost:8025

## Cách chạy trên VPS (k3s)

```bash
# Mailpit (đã có manifest k8s/infra/mailpit.yaml)
kubectl apply -f k8s/infra/mailpit.yaml

# notification-service: image build qua CI (docker build -f Dockerfile
# --build-arg SERVICE=notification-service), deploy như service khác
kubectl apply -f k8s/notification/
```

Xem UI Mailpit từ máy local qua SSH tunnel:
```bash
ssh -L 8025:localhost:8025 root@162.4.177.91 -p 24700
kubectl port-forward -n ecommerce svc/mailpit 8025:8025
# mở http://localhost:8025
```

## Load test hàng trăm nghìn mail

`tools/loadtest_notification.py` — 2 chế độ:

```bash
# 1. End-to-end qua Kafka (đúng flow production: consumer + worker + Mailpit)
pip install kafka-python-ng
python tools/loadtest_notification.py kafka --count 100000 --bootstrap 162.4.177.91:9092

# 2. SMTP thẳng vào Mailpit (đo throughput thuần của Mailpit)
python tools/loadtest_notification.py smtp --count 100000 --host 127.0.0.1 --port 1025
# (port-forward trước: kubectl port-forward -n ecommerce svc/mailpit 1025:1025)
```

Kafka mode bắn event `OrderConfirmed` thật (cùng shape order-service sẽ publish),
recipient `load-user-{i%1000}` — 1000 user lặp lại, mỗi user nhận ~100 mail.

**Lưu ý**: nếu chưa cấu hình `KEYCLOAK_ADMIN_CLIENT_ID/SECRET`, các request sẽ ở
`FAILED_RETRYABLE` với lý do `RECIPIENT_UNRESOLVED` (không gửi mù). Để demo có mail
vào inbox Mailpit, cần service account Keycloak có role `view-users`, hoặc dùng
chế độ SMTP để đo Mailpit thuần.

## Metrics (Prometheus /actuator/prometheus)

- `notification_send_accepted`, `notification_send_unknown`, `notification_send_unknown_recovered`
- `notification_send_retryable`, `notification_send_final`, `notification_send_exhausted`
- `notification_send_processing` (timer theo outcome), `notification_dlq`

## REST API

- `GET /api/v1/me/notifications` — thông báo của principal hiện tại
- `GET /api/v1/admin/notifications/jobs` — đếm theo status (ADMIN)
- `GET /api/v1/admin/notifications/dlq` — DLQ đang mở (ADMIN)
- `GET /api/v1/admin/notifications/orders/{orderId}` — truy vết theo đơn (ADMIN)

## Rollback

- Image deploy theo tag SHA (như service khác) — rollback = `kubectl rollout undo
  deploy/notification-service` hoặc set image về SHA trước.
- Migration Flyway V1 chỉ CREATE TABLE (không ALTER) — rollback schema:
  `DROP TABLE notification_dead_letter, notification_delivery_attempt,
  notification_inbox, notification_request CASCADE` (mất lịch sử gửi — chấp nhận
  được với môi trường học/UAT; không rollback schema khi chỉ rollback code).
- Mailpit: `kubectl delete -f k8s/infra/mailpit.yaml` (giữ PVC nếu muốn giữ mail).

## Còn thiếu (theo docs, chưa làm trong slice này)

- SMS channel (docs: sms-provider-fake) — hiện chỉ EMAIL
- Template versioning + duyệt mẫu (NOTI-02/UC04), consent marketing (NOTI-UC03),
  chiến dịch Quartz (NOTI-UC06), retry thủ công từ DLQ (NOTI-UC05)
- Testcontainers integration test (DoD yêu cầu; hiện mới unit test)
