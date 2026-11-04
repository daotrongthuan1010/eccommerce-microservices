package com.dtthuan3.ecommerce.inventoryservice.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Giao diện lưu hạn mức nhận trả và biên nhận parcel; chi tiết SQL nằm trong adapter JDBC.
 * Interface này là ranh giới truy cập dữ liệu cho service, gom các thao tác đọc/ghi cần thiết. Cách thực
 * thi bằng JPA hay JDBC nằm trong repository/adapter, không đặt trong controller.
 */
public interface InventoryReturnReceiptRepository {
    /** Khóa projection phê duyệt; không tạo hạn mức từ payload của request nhận hàng. */
    Optional<ReturnLineProgress> lockProgress(String returnRequestId, String orderLineId);

    /** Lưu quyết định phê duyệt có version từ event nguồn Return/Order trong cùng transaction với inbox. */
    ApprovalApplyResult applyApproval(ApprovedReturnLine approval);

    Optional<ReturnReceiptRecord> findReceipt(String returnRequestId, String orderLineId,
                                              String parcelIdentity);

    /** Tra cứu biên nhận theo ID mà không khóa dòng để phục vụ màn hình theo dõi. */
    Optional<ReturnReceiptRecord> findReceiptById(String receiptId);

    /** Đọc và khóa biên nhận theo ID để tuần tự hóa việc kiểm tra cùng một parcel. */
    Optional<ReturnReceiptRecord> findReceiptByIdForUpdate(String receiptId);

    ReturnReceiptRecord insertReceipt(ReturnReceiptRecord receipt);

    boolean addReceivedQuantity(String returnRequestId, String orderLineId, BigDecimal quantity);

    /**
     * Tiến độ số lượng được duyệt và số lượng đã nhận của một dòng hàng trả.
     * Record này là projection/value object mang đúng các cột cần thiết giữa repository và service. Nó không
     * tạo quan hệ JPA; việc khóa, lưu và cập nhật dữ liệu vẫn được thực hiện bởi repository.
     */
    record ReturnLineProgress(String enterpriseId, String sku, BigDecimal approvedQuantity,
                              BigDecimal receivedQuantity, Long sourceVersion, String approvalEventId) {}

    /** Snapshot đã được phê duyệt ở Return/Order; không lấy approvedQuantity từ ReceiveReturnReceiptRequest. */
    record ApprovedReturnLine(String returnRequestId, String orderLineId, String enterpriseId,
                              String sku, BigDecimal approvedQuantity, long sourceVersion,
                              String approvalEventId) {}

    /** Kết quả apply event để listener phân biệt event mới, replay cùng version hoặc dữ liệu xung đột. */
    enum ApprovalApplyResult { APPLIED, DUPLICATE, STALE, CONFLICT }

    /**
     * Dữ liệu biên nhận parcel hàng trả, gồm vị trí cách ly và phiên bản tồn sau khi nhận.
     * Record này là projection/value object mang đúng các cột cần thiết giữa repository và service. Nó không
     * tạo quan hệ JPA; việc khóa, lưu và cập nhật dữ liệu vẫn được thực hiện bởi repository.
     */
    record ReturnReceiptRecord(String id, String returnRequestId, String orderLineId,
                               String parcelIdentity, String inventoryId, String enterpriseId,
                               String warehouseId, String locationId, String sku,
                               BigDecimal approvedQuantity, BigDecimal receivedQuantity,
                               BigDecimal lineReceivedAfter, Long inventoryVersionAfter,
                               String performedBy, String reason, LocalDateTime createdAt) {}
}
