# ADR 0002: Mọi event saga phải qua transactional outbox — cấm publish Kafka trong transaction

- **Trạng thái**: Chấp nhận
- **Ngày**: 2026-11-07
- **Refs**: issue #5

## Bối cảnh

Audit phát hiện `PaymentEventConsumer.publishOrderConfirmed` gửi Kafka trực tiếp trong transaction DB (không qua outbox) và nuốt exception — nếu Kafka send fail, event `OrderConfirmed` mất vĩnh viễn: mail xác nhận không bao giờ gửi, không có cơ chế nào phát hiện. Đây là dual-write — chính là anti-pattern mà outbox tồn tại để loại trừ.

## Quyết định

Quy tắc cứng: **không có lệnh `kafkaTemplate.send` nào trong cùng method `@Transactional` với ghi DB.** Mọi event phát từ consumer/service phải ghi vào bảng outbox của service đó, trong cùng transaction với business write; publisher poller riêng đẩy lên Kafka.

Ngoại lệ duy nhất: reply cho HTTP request không có ghi DB (pure transformation) được send trực tiếp.

## Lý do

- Outbox pattern đã có sẵn ở 7 service, kể cả bản nâng cao (FOR UPDATE SKIP LOCKED + lease token + retry backoff ở inventory) — chi phí tuân thủ gần như bằng 0.
- Sửa từng chỗ (wrap send trong try-catch) không giải quyết được: process crash giữa commit DB và send vẫn mất event; chỉ outbox loại trừ được cả hai.
- Nuốt exception quanh send khiến mất event vô hình — không log, không metric, không DLT.

## Hệ quả

- Consumer xử lý event và phát event tiếp theo trở thành "mini-transaction": ghi kết quả + ghi outbox cùng commit.
- Reviewer có checklist đơn giản: grep `kafkaTemplate.send` trong `@Transactional` = bug.
- Latency event tăng tối đa bằng chu kỳ poll (5s) — chấp nhận cho notification, không ảnh hưởng nghiệp vụ.
