package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReportInventoryShortageRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryShortageCaseResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryShortageCaseDetailResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.Inventory;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryShortageRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryShortageRepository.ShortageCaseRecord;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryLocationAccessService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryShortageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Kiểm tra dữ liệu shortage theo allocation và lưu case bất biến để các bước sau tiếp tục xử lý. */
@Service
@RequiredArgsConstructor
public class InventoryShortageServiceImpl implements InventoryShortageService {
    private final InventoryRepository inventoryRepository;
    private final InventoryShortageRepository shortageRepository;
    private final InventoryLocationAccessService locationAccessService;

    /** Lấy trạng thái case cùng lịch sử reserve thay thế để phục vụ theo dõi workflow. */
    @Override
    @Transactional(readOnly = true)
    public InventoryShortageCaseDetailResponse getById(String caseId) {
        return shortageRepository.findDetail(caseId.trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy shortage case: " + caseId));
    }

    /** Lọc danh sách shortage theo doanh nghiệp/trạng thái với truy vấn phân trang ở database. */
    @Override
    @Transactional(readOnly = true)
    public Page<InventoryShortageCaseResponse> list(String enterpriseId, String status, Pageable pageable) {
        if (enterpriseId == null || enterpriseId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "enterpriseId là bắt buộc");
        }
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim().toUpperCase();
        return shortageRepository.search(enterpriseId.trim(), normalizedStatus, pageable);
    }

    /** Lưu shortage một lần; retry đồng nhất nhận lại case cũ theo operationId. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryShortageCaseResponse report(ReportInventoryShortageRequest request, String reportedBy) {
        String inventoryId = request.inventoryId().trim();
        String operationId = request.operationId().trim();
        String orderId = request.orderId().trim();
        String orderLineId = request.orderLineId().trim();
        String allocationId = request.allocationId().trim();
        String reason = request.reason().trim();

        // Kiểm tra hình học lượng trước khi khóa hoặc ghi dữ liệu để báo lỗi gần với input sai.
        BigDecimal undispatched = request.committedQuantity().subtract(request.dispatchedQuantity());
        if (undispatched.signum() < 0 || request.shortageQuantity().compareTo(undispatched) > 0) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY,
                    "shortageQuantity phải nằm trong phần allocation chưa dispatch");
        }

        Inventory inventory = inventoryRepository.findInventoryRowById(inventoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy Inventory nguồn: " + inventoryId));
        String payloadHash = payloadHash(inventoryId, orderId, orderLineId, allocationId,
                request.committedQuantity(), request.dispatchedQuantity(), request.shortageQuantity(), reason);
        InventoryShortageCaseResponse existing = replayIfPresent(inventory.getEnterpriseId(), operationId,
                payloadHash).orElse(null);
        if (existing != null) return existing;

        // Đọc và khóa balance nguồn, sau đó xác nhận kho/vị trí vẫn được phép ghi nghiệp vụ.
        inventory = inventoryRepository.findInventoryRowByIdForUpdate(inventoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy Inventory nguồn: " + inventoryId));
        locationAccessService.requireWritableInventory(inventory);
        existing = replayIfPresent(inventory.getEnterpriseId(), operationId, payloadHash).orElse(null);
        if (existing != null) return existing;

        String actor = reportedBy == null || reportedBy.isBlank() ? "unknown" : reportedBy.trim();
        LocalDateTime now = LocalDateTime.now();
        ShortageCaseRecord record = new ShortageCaseRecord(UUID.randomUUID().toString(),
                inventory.getEnterpriseId(), inventory.getWarehouseId(), inventory.getId(), operationId,
                payloadHash, orderId, orderLineId, allocationId, request.committedQuantity(),
                request.dispatchedQuantity(), request.shortageQuantity(), "OPEN", reason, actor,
                inventory.getVersion(), now);
        if (shortageRepository.insert(record)) return toResponse(record);

        // Unique constraint enterprise + operationId is the final guard for concurrent duplicate requests.
        return replayIfPresent(inventory.getEnterpriseId(), operationId, payloadHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT,
                        "Báo cáo thiếu hàng đang được xử lý; hãy gửi lại cùng operationId"));
    }

    private Optional<InventoryShortageCaseResponse> replayIfPresent(String enterpriseId, String operationId,
                                                                    String expectedHash) {
        Optional<InventoryShortageCaseResponse> existing = shortageRepository.findByOperation(enterpriseId,
                operationId);
        if (existing.isEmpty()) return Optional.empty();
        String storedHash = shortageRepository.findPayloadHash(enterpriseId, operationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "Không đọc được dấu idempotency của shortage case"));
        if (!MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII))) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "operationId đã được dùng cho báo cáo thiếu hàng với dữ liệu khác");
        }
        return existing;
    }

    private InventoryShortageCaseResponse toResponse(ShortageCaseRecord record) {
        return new InventoryShortageCaseResponse(record.id(), record.operationId(), record.enterpriseId(),
                record.warehouseId(), record.inventoryId(), record.orderId(), record.orderLineId(),
                record.allocationId(), record.committedQuantity(), record.dispatchedQuantity(),
                record.shortageQuantity(), record.status(), record.reason(), record.reportedBy(),
                record.createdAt());
    }

    /** Hash payload nghiệp vụ chuẩn hóa để caller retry được nhưng không thể đổi dữ liệu dưới cùng key. */
    private String payloadHash(String inventoryId, String orderId, String orderLineId, String allocationId,
                               BigDecimal committed, BigDecimal dispatched, BigDecimal shortage, String reason) {
        String canonical = String.join("\n", inventoryId, orderId, orderLineId, allocationId,
                committed.stripTrailingZeros().toPlainString(), dispatched.stripTrailingZeros().toPlainString(),
                shortage.stripTrailingZeros().toPlainString(), reason);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không có sẵn trong JVM", exception);
        }
    }
}
