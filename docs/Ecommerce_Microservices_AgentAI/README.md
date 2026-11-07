# E-commerce Microservices — bộ đặc tả cho AI Coding Agent

> Ngôn ngữ: tiếng Việt. Đầu ra: tập hợp Markdown (`.md`), **chưa phải source code**.

## Đọc tài liệu theo thứ tự

1. [`AGENTS.md`](AGENTS.md): quy tắc bắt buộc cho coding agent.
2. [`docs/00-pham-vi-va-nguyen-tac.md`](docs/00-pham-vi-va-nguyen-tac.md): phân biệt **bán lẻ một chủ hàng** và **sàn đa nhà bán**.
3. [`plans/00-lo-trinh.md`](plans/00-lo-trinh.md): làm theo các giai đoạn; không code toàn hệ thống cùng lúc.
4. [`backend/README.md`](backend/README.md), [`frontend/README.md`](frontend/README.md), [`shared/README.md`](shared/README.md).
5. [`marketplace/README.md`](marketplace/README.md) nếu muốn có nhiều người bán như các sàn.
6. [`reference/README.md`](reference/README.md): toàn bộ tài liệu DOCX gốc, được chuyển đủ **50 chương + 4 phụ lục**.

## Kết cấu

```text
AGENTS.md                  Quy định chung cho agent
backend/                   Kiến trúc + 14 service gốc + hướng dẫn BE
frontend/                  Thiết kế FE; customer, seller, admin, ops
shared/                    Hợp đồng REST/event, trạng thái, lỗi, quyền
marketplace/               Mở rộng marketplace — chỉ bật ở giai đoạn riêng
qa/                        Kiểm thử nghiệp vụ, bảo mật, hiệu năng
plans/                     Thứ tự triển khai và checklist theo giai đoạn
reference/                 Markdown chuyển từ DOCX, nguyên vẹn nội dung
```

## Mốc triển khai

- **P0:** chạy được backend/frontend, đăng nhập, tìm kiếm, catalog, giỏ, đơn mẫu.
- **P1:** nghiệp vụ bán lẻ nhiều kho với giữ tồn, COD/online giả lập, Saga, đổi trả, kiểm thử cạnh tranh.
- **P2:** thêm seller/store/offer và tách đơn theo nhà bán nếu quyết định xây marketplace.
- **P3:** tài chính marketplace: hoa hồng, sao kê, đối soát, thanh toán cho nhà bán và xử lý tranh chấp.

Không chép giao diện/nhãn hiệu/hình ảnh độc quyền của Shopee, Tiki hay Lazada. Tham khảo **luồng và nhóm chức năng**; thiết kế nhận diện riêng.

## Phân loại yêu cầu

- **[NGUỒN]:** căn cứ từ Word và liên kết chương gốc.
- **[ĐỀ XUẤT]:** thiết kế hợp đồng API, route, cấu trúc code hoặc nghiệp vụ marketplace chưa có trong Word; phải được người phụ trách dự án duyệt trước khi triển khai production.
- **[CHƯA CHỐT]:** điều kiện luật/phí/hạn mức tích hợp thực tế cần được quyết định rõ; không tự chế số liệu.
