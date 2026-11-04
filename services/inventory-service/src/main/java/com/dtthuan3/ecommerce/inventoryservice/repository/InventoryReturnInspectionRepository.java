package com.dtthuan3.ecommerce.inventoryservice.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Cổng JDBC để đọc/ghi kết quả kiểm tra parcel; service không phụ thuộc cách truy cập database.
 * Interface này là ranh giới truy cập dữ liệu cho service, gom các thao tác đọc/ghi cần thiết. Cách thực
 * thi bằng JPA hay JDBC nằm trong repository/adapter, không đặt trong controller.
 */
public interface InventoryReturnInspectionRepository {

    /** Tìm kết quả đã chốt theo biên nhận, dùng để xử lý retry và ngăn kiểm tra lần hai. */
    Optional<InspectionRecord> findByReceiptId(String receiptId);

    /** Lưu một kết quả kiểm tra và trả thời gian ghi do database cấp. */
    InspectionRecord insert(InspectionRecord inspection);

    /**
     * Dữ liệu kiểm tra đã lưu, dùng làm nguồn trả response nhất quán cho các lần retry.
     * Record này là projection/value object mang đúng các cột cần thiết giữa repository và service. Nó không
     * tạo quan hệ JPA; việc khóa, lưu và cập nhật dữ liệu vẫn được thực hiện bởi repository.
     */
    record InspectionRecord(String id, String receiptId, String operationId, String inventoryId,
                            String sku, BigDecimal receivedQuantity, BigDecimal goodQuantity,
                            BigDecimal damagedQuantity, Long inventoryVersionAfter,
                            String inspectedBy, String reason, LocalDateTime inspectedAt) {}
}
