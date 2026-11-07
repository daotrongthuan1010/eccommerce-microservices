# Glossary

Thuật ngữ nghiệp vụ ecommerce — không chứa chi tiết triển khai.

## Order (Đơn hàng)

- **Order**: đơn đặt hàng của Customer; vòng đời: `PENDING_PAYMENT → PAID → CONFIRMED → SHIPPED → DELIVERED`, nhánh lỗi: `PAYMENT_FAILED`, `CANCELLED`, `EXPIRED`.
- **OrderStatus**: trạng thái hợp lệ của Order tại một thời điểm; chỉ chuyển qua transition được định nghĩa trước (state machine), không gán string tự do.
- **Idempotency-Key**: khóa client gửi kèm request tạo Order/Payment/Shipping; cùng khóa + cùng payload phải trả cùng kết quả, không tạo bản ghi thứ hai.
- **Request hash**: vân tay SHA-256 của payload; cùng Idempotency-Key nhưng khác payload là xung đột, từ chối.

## Saga

- **Checkout Saga**: chuỗi tự điều phối (choreography) Order → Payment → Inventory → Shipping, mỗi bước giao tiếp qua event Kafka; Order giữ vai trò trung tâm theo dõi trạng thái.
- **Compensating action**: hành động hoàn tác khi một bước saga fail — ví dụ Payment fail thì Release Reservation để trả hàng đang giữ.
- **Compensation gap**: tình trạng bước fail nhưng không có compensating action chạy — hàng bị giữ vô ích đến khi TTL hết hạn.

## Inventory (Tồn kho)

- **Reservation**: lời giữ hàng cho một Order, TTL mặc định 15 phút; các trạng thái HELD → COMMITTED / RELEASED / EXPIRED.
- **Available**: lượng khả dụng = physical − reserved − defective − pending; chỉ đủ điều kiện reserve khi > 0.
- **Oversell**: bán vượt tồn thực — ràng buộc bất biến: `reserved ≤ physical` mọi lúc.
- **Release**: hoàn đúng lượng đã giữ của một Reservation (idempotent theo operationId).

## Outbox / Event

- **Transactional Outbox**: event được ghi vào bảng outbox trong cùng transaction với dữ liệu nghiệp vụ; publisher riêng quét và đẩy lên Kafka — loại trừ dual-write.
- **Dual-write**: anti-pattern ghi DB rồi publish message ngoài transaction — một trong hai có thể mất.
- **Inbox (dedup)**: bảng ghi event_id đã xử lý để consumer bỏ qua bản trùng lặp từ Kafka.
- **DLT (Dead Letter Topic)**: topic Kafka chứa message xử lý fail sau khi hết retry, chờ can thiệp/replay thủ công.
- **Poison message**: message luôn gây lỗi khi xử lý; không được nuốt exception — phải đẩy DLT.

## Flash Sale

- **Hot row**: một dòng inventory bị hàng nghìn transaction tranh giữ — điểm nghẽn serialize của flash sale.
- **Redis pre-check**: lớp lọc nhanh trước khi vào DB (kiểm tra còn hàng từ cache) — giảm tải DB nhưng không phải trọng tài cuối.
- **Rate limiting**: giới hạn request/giô từ gateway trước khi tới service, bảo vệ hạ tầng khi flash sale.
