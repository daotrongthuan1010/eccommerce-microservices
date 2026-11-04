package com.dtthuan3.ecommerce.inventoryservice.repository;

import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryShortageCaseResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ShortageReallocationReservationResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryShortageCaseDetailResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Cổng ghi/đọc hồ sơ thiếu hàng; service không phụ thuộc chi tiết câu SQL.
 * Interface này là ranh giới truy cập dữ liệu cho service, gom các thao tác đọc/ghi cần thiết. Cách thực
 * thi bằng JPA hay JDBC nằm trong repository/adapter, không đặt trong controller.
 */
public interface InventoryShortageRepository {
    Optional<InventoryShortageCaseResponse> findByOperation(String enterpriseId, String operationId);
    Optional<InventoryShortageCaseDetailResponse> findDetail(String caseId);
    Page<InventoryShortageCaseResponse> search(String enterpriseId, String status, Pageable pageable);
    Optional<String> findPayloadHash(String enterpriseId, String operationId);
    boolean insert(ShortageCaseRecord record);
    Optional<ShortageCaseState> lockShortageCase(String caseId);
    BigDecimal sumReallocatedQuantity(String caseId);
    Optional<ReallocationRecord> findReallocation(String caseId, String operationId);
    boolean insertReallocation(ReallocationRecord record);
    boolean updateShortageStatus(String caseId, String expectedStatus, String newStatus);

    /**
     * Snapshot vụ thiếu hàng gắn với reservation/order line để theo dõi và phân bổ bù.
     * Record này là projection/value object mang đúng các cột cần thiết giữa repository và service. Nó không
     * tạo quan hệ JPA; việc khóa, lưu và cập nhật dữ liệu vẫn được thực hiện bởi repository.
     */
    record ShortageCaseRecord(String id, String enterpriseId, String warehouseId, String inventoryId,
                              String operationId, String payloadHash, String orderId, String orderLineId,
                              String allocationId, BigDecimal committedQuantity, BigDecimal dispatchedQuantity,
                              BigDecimal shortageQuantity, String status, String reason, String reportedBy,
                              long inventoryVersion, LocalDateTime createdAt) {}

    /**
     * Trạng thái tối thiểu của vụ thiếu hàng dùng khi khóa và kiểm tra cập nhật nghiệp vụ.
     * Record này là projection/value object mang đúng các cột cần thiết giữa repository và service. Nó không
     * tạo quan hệ JPA; việc khóa, lưu và cập nhật dữ liệu vẫn được thực hiện bởi repository.
     */
    record ShortageCaseState(String id, String enterpriseId, String inventoryId,
                             BigDecimal shortageQuantity, String status) {}

    /**
     * Kết quả một lần phân bổ lại tồn, lưu cả lượng đã phân bổ và lượng thiếu còn lại.
     * Record này là projection/value object mang đúng các cột cần thiết giữa repository và service. Nó không
     * tạo quan hệ JPA; việc khóa, lưu và cập nhật dữ liệu vẫn được thực hiện bởi repository.
     */
    record ReallocationRecord(String id, String shortageCaseId, String operationId, String reallocationId,
                              String targetInventoryId, BigDecimal quantity,
                              BigDecimal totalReallocatedAfter, BigDecimal remainingShortageAfter,
                              String shortageStatusAfter, String payloadHash, String reservedBy,
                              long targetInventoryVersion, LocalDateTime createdAt) {
        public ShortageReallocationReservationResponse toResponse() {
            return new ShortageReallocationReservationResponse(shortageCaseId, operationId, reallocationId,
                    targetInventoryId, quantity, totalReallocatedAfter, remainingShortageAfter,
                    shortageStatusAfter, targetInventoryVersion);
        }
    }
}
