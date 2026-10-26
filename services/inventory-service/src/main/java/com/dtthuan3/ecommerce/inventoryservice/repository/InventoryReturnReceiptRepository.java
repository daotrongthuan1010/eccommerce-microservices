package com.dtthuan3.ecommerce.inventoryservice.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/** Giao diện lưu hạn mức nhận trả và biên nhận parcel; chi tiết SQL nằm trong adapter JDBC. */
public interface InventoryReturnReceiptRepository {
    ReturnLineProgress lockOrCreateProgress(String returnRequestId, String orderLineId,
                                           BigDecimal approvedQuantity);

    Optional<ReturnReceiptRecord> findReceipt(String returnRequestId, String orderLineId,
                                              String parcelIdentity);

    /** Tra cứu biên nhận theo ID mà không khóa dòng để phục vụ màn hình theo dõi. */
    Optional<ReturnReceiptRecord> findReceiptById(String receiptId);

    /** Đọc và khóa biên nhận theo ID để tuần tự hóa việc kiểm tra cùng một parcel. */
    Optional<ReturnReceiptRecord> findReceiptByIdForUpdate(String receiptId);

    ReturnReceiptRecord insertReceipt(ReturnReceiptRecord receipt);

    boolean addReceivedQuantity(String returnRequestId, String orderLineId, BigDecimal quantity);

    record ReturnLineProgress(BigDecimal approvedQuantity, BigDecimal receivedQuantity) {}

    record ReturnReceiptRecord(String id, String returnRequestId, String orderLineId,
                               String parcelIdentity, String inventoryId, String enterpriseId,
                               String warehouseId, String locationId, String sku,
                               BigDecimal approvedQuantity, BigDecimal receivedQuantity,
                               BigDecimal lineReceivedAfter, Long inventoryVersionAfter,
                               String performedBy, String reason, LocalDateTime createdAt) {}
}
