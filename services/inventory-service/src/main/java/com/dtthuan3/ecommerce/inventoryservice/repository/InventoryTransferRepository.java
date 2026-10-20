package com.dtthuan3.ecommerce.inventoryservice.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Cổng lưu phiếu điều chuyển; service chỉ biết interface, SQL được đặt trong adapter JDBC. */
public interface InventoryTransferRepository {
    /** Tìm phiếu theo khóa idempotency để trả kết quả cũ hoặc phát hiện payload không khớp. */
    Optional<TransferHeaderRecord> findByOperationId(String operationId);

    /** Khóa header trong transaction để dispatch đồng thời cùng phiếu được tuần tự hóa. */
    Optional<TransferHeaderRecord> findByIdForUpdate(String transferId);

    /** Claim operationId bằng insert nguyên tử; false nghĩa là request cạnh tranh đã tạo phiếu trước. */
    boolean insertHeaderIfAbsent(TransferHeaderRecord header);

    /** Batch insert các dòng phiếu trong transaction đang tạo header. */
    void insertLines(String transferId, List<TransferLineRecord> lines);

    /** Đọc các dòng theo line_number để response giữ đúng thứ tự người dùng khai báo. */
    List<TransferLineRecord> findLines(String transferId);

    /** Cập nhật dữ liệu audit tồn nguồn cho toàn bộ line; trả false nếu line không còn khớp. */
    boolean markLinesDispatched(String transferId, List<TransferLineRecord> lines);

    /** Compare-and-set CREATED sang IN_TRANSIT và lưu khóa retry/audit trong cùng câu UPDATE. */
    boolean markDispatched(String transferId, long expectedVersion, String operationId,
                           String dispatchedBy, Instant dispatchedAt);

    /** Cộng các bucket đã nhận vào dòng phiếu nếu lượng đối soát không vượt phần còn đang vận chuyển. */
    boolean markLinesReceived(String transferId, List<ReceiptProgressUpdate> updates);

    /** Compare-and-set IN_TRANSIT/PARTIALLY_RECEIVED sang trạng thái sau lần nhận. */
    boolean markReceived(String transferId, long expectedVersion, String statusAfter);

    /** Tìm lần nhận để replay an toàn khi client gửi lại operationId. */
    Optional<TransferReceiptOperationRecord> findReceiptOperation(String transferId, String operationId);

    /** Lưu snapshot idempotency của lần nhận; dòng chi tiết được ghi cùng transaction. */
    TransferReceiptOperationRecord insertReceiptOperation(TransferReceiptOperationRecord operation);

    /** Lưu kết quả kiểm đếm từng dòng để phục vụ retry, sai lệch và audit. */
    void insertReceiptOperationLines(List<TransferReceiptOperationLineRecord> lines);

    /** Đọc lại chi tiết đã lưu để trả đúng response gốc khi retry operationId. */
    List<TransferReceiptOperationLineRecord> findReceiptOperationLines(String operationId);

    /** Dữ liệu header tối thiểu truyền giữa adapter JDBC và service. */
    record TransferHeaderRecord(String id, String enterpriseId,
                                String sourceWarehouseId, String sourceLocationId,
                                String destinationWarehouseId, String destinationLocationId,
                                String operationId, String payloadHash, String referenceCode,
                                String reason, String status, long version,
                                String createdBy, Instant createdAt, String dispatchOperationId,
                                Long dispatchExpectedVersion, String dispatchedBy, Instant dispatchedAt) {}

    /** Dữ liệu một dòng hàng đã lưu. */
    record TransferLineRecord(String lineId, int lineNumber, String sku, BigDecimal plannedQuantity,
                              String sourceInventoryId, BigDecimal dispatchedQuantity,
                              Long sourceInventoryVersionAfter, BigDecimal receivedGoodQuantity,
                              BigDecimal receivedDamagedQuantity, BigDecimal missingQuantity) {}

    /** Cộng dồn số liệu nhận theo dòng với điều kiện database vẫn còn đủ lượng chưa đối soát. */
    record ReceiptProgressUpdate(String lineId, BigDecimal receivedGoodQuantity,
                                 BigDecimal receivedDamagedQuantity, BigDecimal missingQuantity,
                                 BigDecimal accountedQuantity) {}

    /** Snapshot của một operation nhận, gồm trạng thái/version đã trả về lần đầu. */
    record TransferReceiptOperationRecord(String id, String transferId, String operationId,
                                          String payloadHash, String statusAfter,
                                          Long transferVersionAfter, String receivedBy,
                                          Instant receivedAt) {}

    /** Kết quả từng dòng đã lưu để retry không đọc số dư có thể đã thay đổi về sau. */
    record TransferReceiptOperationLineRecord(String id, String operationId, String transferLineId,
                                               String sku, BigDecimal goodQuantity,
                                               BigDecimal damagedQuantity, BigDecimal missingQuantity,
                                               String discrepancyReason, String sourceInventoryId,
                                               Long sourceInventoryVersionAfter,
                                               String destinationInventoryId,
                                               Long destinationInventoryVersionAfter) {}
}
