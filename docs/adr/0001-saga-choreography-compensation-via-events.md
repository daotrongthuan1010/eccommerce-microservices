# ADR 0001: Saga choreography với Order làm orchestrator, compensation qua event

- **Trạng thái**: Chấp nhận
- **Ngày**: 2026-11-07
- **Refs**: issue #4

## Bối cảnh

Checkout Saga trải dài order → payment → inventory → shipping. Hai lựa chọn phổ biến: **orchestration** (một Saga orchestrator điều phối từng bước qua command) hoặc **choreography** (mỗi service tự phản ứng event, không bộ điều phối trung tâm). Dự án đang chạy choreography: order tạo `PENDING_PAYMENT` + outbox, payment consume `order-events`, order consume `payment-events`.

Vấn đề phát hiện khi audit: payment fail không có compensating action, và `InventoryClient` (Feign sync) được inject nhưng không bao giờ gọi — hệ thống lưng chừng giữa 2 phong cách.

## Quyết định

Giữ **choreography**, bỏ hướng sync Feign:

1. Compensation chạy qua **event chuỗi**, không phải HTTP: khi payment fail → order ghi event `OrderPaymentFailed` vào outbox (cùng transaction với update status) → inventory consume và **release reservation**.
2. Xóa `InventoryClient`/`PaymentClient` dead code khỏi order-service. Không thêm phụ thuộc runtime sync giữa các bước saga.
3. `OrderStatus` là enum + state machine tường minh; mọi transition qua một chỗ duy nhất.

## Lý do

- Sync Feign giữa bước saga tạo chuỗi phụ thuộc thời gian chạy và một điểm fail mới (network) đúng chỗ đang cần rollback — phản tác dụng.
- Choreography + event compensation đã là pattern hệ thống đang dùng (inbox dedup, outbox sẵn ở 7 service); đi cùng chiều đó thì mỗi fix tái dùng hạ tầng có sẵn.
- Với 16 service, một orchestrator riêng sẽ là service thứ 17 phải deploy/monitor; không xứng đáng cho quy mô này.

## Hệ quả

- Chuỗi compensation dài hơn (2 hop Kafka thay vì 1 call) — chấp nhận vì reservation TTL 15 phút là lưới an toàn cuối.
- Mọi bước saga phải có consumer idempotent + inbox (đã có) và event phải qua outbox (bắt buộc sau ADR 0002).
- Shipping join saga bằng cách consume event như các service khác — không thêm call sync.
