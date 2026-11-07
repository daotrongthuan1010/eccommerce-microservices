# ADR 0003: PostgreSQL là trọng tài tồn kho; Redis chỉ là lớp lọc đọc

- **Trạng thái**: Chấp nhận
- **Ngày**: 2026-11-07
- **Refs**: issue #6, docs/INVENTORY_CASE_SOLUTION_TECH.md INV-05, INV-UC01

## Bối cảnh

Flash sale: hàng trăm nghìn user tranh mua một SKU → mọi reserve dồn vào một row inventory (`SELECT ... FOR UPDATE`). Hai hướng phổ biến: (a) chuyển trọng tài sang Redis (DECR/Lua script atomic) cho tốc độ, hoặc (b) giữ PostgreSQL làm trọng tài, dùng Redis chỉ để lọc request trước khi vào DB.

## Quyết định

**PostgreSQL giữ vai trò trọng tài** — mọi HELD/REJECTED quyết định bởi conditional UPDATE trong DB. Redis chỉ làm 2 việc:

1. **Read cache** khả dụng (đã có `InventoryAvailabilityReadCache`): khi cache báo hết hàng, short-circuit trả `INSUFFICIENT_STOCK` ngay không chạm DB. Khi cache báo còn hàng thì vẫn vào DB — cache không bao giờ dùng để *chấp nhận* đơn.
2. **Rate limiting ở gateway** (RequestRateLimiter theo user/IP) — giảm lưu lượng vào service trước khi chạm tới tầng nghiệp vụ.

Thêm lớp phòng thủ cuối: **CHECK constraint DB** `reserved ≤ physical` — bất biến được CSDL bảo đảm, không phụ thuộc code path nào.

## Lý do

- Redis DECR làm trọng tài tạo nguồn sự thật thứ hai phải reconcile về DB — cả một lớp drift/repair code chỉ để phục vụ một loại event hiếm. Với hệ thống học tập + VPS single-node, chi phí phức tạp đó không đổi được tính đúng đắn.
- Thiết kế INV-05 đã chọn hướng này và có kịch bản test rõ (1000 request đồng thời, Redis tắt vẫn phải đúng nghiệp vụ) — code hiện tại mới thiếu phần lọc, không mâu thuẫn thiết kế.
- Hot-row serialize là vấn đề *throughput*, không phải *correctness*: giải bằng rate limit + cache lọc + (sau này) sharded counter nếu thật sự cần.

## Hệ quả

- Flash sale SKU cực nóng vẫn có thể serialize ở DB — chấp nhận: đúng hơn nhanh, và rate limit giữ trải nghiệm còn hàng/còn lượt.
- Khi Redis down: lưu lượng dồn về DB — cần alert (đã có stack Prometheus/Grafana từ mẻ 2).
- Nếu sau này cần >1k TPS cho một SKU, cân nhắc lại với counter shard + Lua, nhưng phải giữ đường reconcile về PostgreSQL.
