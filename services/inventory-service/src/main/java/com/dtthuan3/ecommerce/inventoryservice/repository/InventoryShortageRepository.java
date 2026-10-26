package com.dtthuan3.ecommerce.inventoryservice.repository;

import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryShortageCaseResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ShortageReallocationReservationResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryShortageCaseDetailResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Cổng ghi/đọc hồ sơ thiếu hàng; service không phụ thuộc chi tiết câu SQL. */
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

    record ShortageCaseRecord(String id, String enterpriseId, String warehouseId, String inventoryId,
                              String operationId, String payloadHash, String orderId, String orderLineId,
                              String allocationId, BigDecimal committedQuantity, BigDecimal dispatchedQuantity,
                              BigDecimal shortageQuantity, String status, String reason, String reportedBy,
                              long inventoryVersion, LocalDateTime createdAt) {}

    record ShortageCaseState(String id, String enterpriseId, String inventoryId,
                             BigDecimal shortageQuantity, String status) {}

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
