package com.dtthuan3.ecommerce.inventoryservice.repository;

import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSessionResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSessionLineResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountRecountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

/**
 * Cổng dữ liệu phiên kiểm kê; service không phụ thuộc cách truy vấn JDBC cụ thể.
 * Interface này là ranh giới truy cập dữ liệu cho service, gom các thao tác đọc/ghi cần thiết. Cách thực
 * thi bằng JPA hay JDBC nằm trong repository/adapter, không đặt trong controller.
 */
public interface InventoryCountSessionRepository {
    Optional<InventoryCountSessionResponse> findByOperation(String enterpriseId, String operationId);
    /** Tải header phiên theo ID để kiểm tra warehouse scope trước mọi API đọc/ghi theo phiên. */
    Optional<InventoryCountSessionResponse> findSessionById(String sessionId);
    Optional<String> findPayloadHash(String enterpriseId, String operationId);
    /** Tìm phiên kiểm kê theo enterprise và bộ lọc tùy chọn với trang dữ liệu giới hạn. */
    Page<InventoryCountSessionResponse> searchSessions(String enterpriseId, String warehouseId,
                                                       String status, Pageable pageable);
    boolean existsSession(String sessionId);
    Page<InventoryCountSessionLineResponse> findLines(String sessionId, Pageable pageable);
    Optional<SessionState> lockSession(String sessionId);
    Optional<CountSubmissionRecord> findSubmission(String sessionId, String operationId);
    long countLinesBelongingToSession(String sessionId, List<String> inventoryIds);
    boolean insertSubmission(CountSubmissionRecord submission);
    int[][] insertObservations(List<CountObservationRecord> observations);
    boolean advanceVersion(String sessionId, long expectedVersion);
    /** Đếm dòng baseline chưa có lần kiểm đếm mới nhất. */
    long countUncountedLines(String sessionId);
    /** Tính chênh lệch theo bucket giữa baseline và lần đếm mới nhất của từng dòng. */
    CountSummary summarizeLatestCounts(String sessionId);
    /** Chuyển OPEN sang PENDING_APPROVAL với điều kiện version để chống submit đồng thời. */
    boolean submitSession(String sessionId, long expectedVersion);
    /** Lấy lần đếm mới nhất cùng balance hiện tại và khóa balance theo thứ tự ID khi duyệt. */
    List<CountApprovalLine> lockApprovalLines(String sessionId);
    /** Chuyển phiên chờ duyệt sang APPROVED sau khi toàn bộ balance và movement đã ghi thành công. */
    boolean approveSession(String sessionId, long expectedVersion, String approvedBy,
                           LocalDateTime approvedAt);
    Optional<CountReviewRecord> findCountReview(String sessionId, String operationId);
    boolean insertCountReview(CountReviewRecord review);
    boolean reopenForRecount(String sessionId, long expectedVersion);

    /** Insert header theo idempotency key; false nghĩa là request đồng thời đã tạo trước. */
    boolean insertSession(InventoryCountSessionRecord session);

    /** Chụp các bucket tồn hiện có vào baseline trong cùng transaction mở phiên. */
    long snapshotInventory(InventoryCountSessionRecord session);

    /**
     * Snapshot header phiên kiểm kê dùng để lưu, khóa và cập nhật trạng thái/version của phiên.
     * Record này là projection/value object mang đúng các cột cần thiết giữa repository và service. Nó không
     * tạo quan hệ JPA; việc khóa, lưu và cập nhật dữ liệu vẫn được thực hiện bởi repository.
     */
    record InventoryCountSessionRecord(String id, String enterpriseId, String warehouseId,
                                       String locationId, String sku, String operationId,
                                       String payloadHash, String status, String reason,
                                       String openedBy, LocalDateTime snapshotAt,
                                       LocalDateTime createdAt, long version) {}

    /**
     * Trạng thái tối thiểu của phiên kiểm kê khi cần kiểm tra chuyển trạng thái có điều kiện.
     * Record này là projection/value object mang đúng các cột cần thiết giữa repository và service. Nó không
     * tạo quan hệ JPA; việc khóa, lưu và cập nhật dữ liệu vẫn được thực hiện bởi repository.
     */
    record SessionState(String id, String status, long version) {}

    /**
     * Tổng hợp số dòng và chênh lệch kiểm kê theo từng loại tồn.
     * Record này là projection/value object mang đúng các cột cần thiết giữa repository và service. Nó không
     * tạo quan hệ JPA; việc khóa, lưu và cập nhật dữ liệu vẫn được thực hiện bởi repository.
     */
    record CountSummary(long lineCount, BigDecimal goodVariance,
                        BigDecimal defectiveVariance, BigDecimal pendingVariance) {}

    /**
     * Dữ liệu so sánh snapshot ban đầu với tồn hiện tại để duyệt kết quả kiểm kê.
     * Record này là projection/value object mang đúng các cột cần thiết giữa repository và service. Nó không
     * tạo quan hệ JPA; việc khóa, lưu và cập nhật dữ liệu vẫn được thực hiện bởi repository.
     */
    record CountApprovalLine(String inventoryId, long snapshotVersion, long currentVersion,
                             BigDecimal currentPhysical, BigDecimal currentReserved,
                             BigDecimal currentDefective, BigDecimal currentPending,
                             BigDecimal currentInTransit,
                             BigDecimal countedGood, BigDecimal countedDefective,
                             BigDecimal countedPending, String countReason) {}
    /**
     * Bản ghi yêu cầu duyệt lại/mở lại phiên kiểm kê, dùng để chống gửi lặp.
     * Record này là projection/value object mang đúng các cột cần thiết giữa repository và service. Nó không
     * tạo quan hệ JPA; việc khóa, lưu và cập nhật dữ liệu vẫn được thực hiện bởi repository.
     */
    record CountReviewRecord(String id, String sessionId, String operationId, String payloadHash,
                             String reason, String requestedBy, long versionAfter,
                             LocalDateTime requestedAt) {
        public InventoryCountRecountResponse toResponse() {
            return new InventoryCountRecountResponse(sessionId, operationId, "OPEN", versionAfter,
                    reason, requestedBy, requestedAt);
        }
    }
    /**
     * Header của một lần gửi kết quả kiểm kê và thông tin idempotency của lần gửi.
     * Record này là projection/value object mang đúng các cột cần thiết giữa repository và service. Nó không
     * tạo quan hệ JPA; việc khóa, lưu và cập nhật dữ liệu vẫn được thực hiện bởi repository.
     */
    record CountSubmissionRecord(String id, String sessionId, String operationId, String payloadHash,
                                 int lineCount, long versionAfter, String recordedBy,
                                 LocalDateTime createdAt) {}
    /**
     * Số lượng thực tế đã đếm cho một Inventory trong một lần gửi kết quả kiểm kê.
     * Record này là projection/value object mang đúng các cột cần thiết giữa repository và service. Nó không
     * tạo quan hệ JPA; việc khóa, lưu và cập nhật dữ liệu vẫn được thực hiện bởi repository.
     */
    record CountObservationRecord(String id, String submissionId, String sessionId, String inventoryId,
                                  BigDecimal countedGoodQuantity,
                                  BigDecimal countedDefectiveQuantity,
                                  BigDecimal countedPendingQuantity,
                                  String countReason, String evidenceReference,
                                  String countedBy, LocalDateTime countedAt) {}
}
