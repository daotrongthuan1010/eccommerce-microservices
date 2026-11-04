package com.dtthuan3.ecommerce.inventoryservice.integration.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Hợp đồng event do Return/Order phát sau khi duyệt hoặc cập nhật hạn mức trả hàng.
 * Producer giữ thứ tự theo khóa returnRequestId:orderLineId, tăng sourceVersion mỗi khi quyết định thay đổi,
 * và chỉ phát event sau khi quyết định đã commit ở service nguồn.
 */
public record ReturnLineApprovalEvent(
        /** ID duy nhất của event, dùng chung với inbox để khử gửi lại. */
        String eventId,
        /** Chỉ chấp nhận ReturnLineApproved hoặc ReturnLineApprovalUpdated. */
        String eventType,
        /** Return request đã được nghiệp vụ duyệt. */
        String returnRequestId,
        /** Order line cụ thể; liên kết ID dòng với SKU và enterprise bên dưới. */
        String orderLineId,
        /** Doanh nghiệp sở hữu order line; Inventory so sánh với vị trí nhận parcel. */
        String enterpriseId,
        /** SKU chuẩn từ order line, không lấy SKU do request nhận hàng tự khai. */
        String sku,
        /** Hạn mức được duyệt; đây là nguồn duy nhất cho tổng lượng Inventory được phép nhận. */
        BigDecimal approvedQuantity,
        /** Version đơn điệu của quyết định trong Return/Order; event cũ không được ghi đè quyết định mới. */
        Long sourceVersion,
        /** Thời điểm nguồn phát quyết định, phục vụ tracing và đối soát. */
        Instant occurredAt
) {
}
