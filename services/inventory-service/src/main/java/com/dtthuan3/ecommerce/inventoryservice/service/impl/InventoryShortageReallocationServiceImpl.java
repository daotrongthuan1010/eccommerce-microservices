package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReserveShortageReallocationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ShortageReallocationReservationResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.Inventory;
import com.dtthuan3.ecommerce.inventoryservice.entity.MovementType;
import com.dtthuan3.ecommerce.inventoryservice.entity.StockMovement;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryShortageRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryShortageRepository.ReallocationRecord;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryShortageRepository.ShortageCaseState;
import com.dtthuan3.ecommerce.inventoryservice.repository.StockMovementRepository;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryLocationAccessService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryShortageReallocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/** Điều phối giữ thay thế, ghi ledger và StockMovement nguyên tử; hỗ trợ retry deadlock an toàn. */
@Service
@RequiredArgsConstructor
public class InventoryShortageReallocationServiceImpl implements InventoryShortageReallocationService {
    private final InventoryShortageRepository shortageRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryLocationAccessService locationAccessService;
    private final DeadlockRetryExecutor deadlockRetryExecutor;

    /** Mỗi lần retry chạy trong transaction mới; idempotency ngăn reserve lại sau commit thành công. */
    @Override
    public ShortageReallocationReservationResponse reserve(String shortageCaseId,
                                                           ReserveShortageReallocationRequest request,
                                                           String reservedBy) {
        String caseId = shortageCaseId.trim();
        String actor = reservedBy == null || reservedBy.isBlank() ? "unknown" : reservedBy.trim();
        return deadlockRetryExecutor.execute("giữ hàng thay thế cho shortage case",
                () -> reserveInTransaction(caseId, request, actor));
    }

    /** Khóa shortage trước, rồi khóa Inventory đích; ledger, balance và outbox commit cùng nhau. */
    private ShortageReallocationReservationResponse reserveInTransaction(String caseId,
                                                                         ReserveShortageReallocationRequest request,
                                                                         String actor) {
        String operationId = request.operationId().trim();
        String reallocationId = request.reallocationId().trim();
        String targetInventoryId = request.targetInventoryId().trim();
        String payloadHash = payloadHash(caseId, operationId, reallocationId, targetInventoryId,
                request.quantity(), request.expectedVersion());

        ReallocationRecord existing = shortageRepository.findReallocation(caseId, operationId).orElse(null);
        if (existing != null) return replayOrReject(existing, payloadHash);

        ShortageCaseState shortage = shortageRepository.lockShortageCase(caseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy shortage case: " + caseId));
        // Request khác cùng operationId có thể đã commit trong lúc request này chờ khóa case.
        existing = shortageRepository.findReallocation(caseId, operationId).orElse(null);
        if (existing != null) return replayOrReject(existing, payloadHash);
        if (!"OPEN".equals(shortage.status())) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Shortage case không còn nhận giữ hàng thay thế: " + shortage.status());
        }

        BigDecimal alreadyReallocated = shortageRepository.sumReallocatedQuantity(caseId);
        BigDecimal remaining = shortage.shortageQuantity().subtract(alreadyReallocated);
        if (request.quantity().compareTo(remaining) > 0) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY,
                    "quantity vượt lượng thiếu còn cần tái phân bổ: " + remaining);
        }

        Inventory source = inventoryRepository.findInventoryRowById(shortage.inventoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy Inventory nguồn của shortage case"));
        Inventory target = inventoryRepository.findInventoryRowByIdForUpdate(targetInventoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy Inventory thay thế: " + targetInventoryId));
        if (source.getId().equals(target.getId())) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "Inventory thay thế phải khác Inventory nguồn đang thiếu");
        }
        if (!shortage.enterpriseId().equals(target.getEnterpriseId())) {
            throw new BusinessException(ErrorCode.RESOURCE_OWNERSHIP_MISMATCH,
                    "Inventory thay thế phải thuộc cùng enterprise với shortage case");
        }
        if (!source.getSku().equals(target.getSku())) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "Inventory thay thế phải chứa cùng SKU với Inventory nguồn");
        }
        if (target.getVersion().longValue() != request.expectedVersion().longValue()) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Inventory thay thế đã thay đổi; hãy tải lại số lượng khả dụng và version mới nhất");
        }
        locationAccessService.requireWritableInventory(target);

        Inventory updated = inventoryRepository.reserve(targetInventoryId, request.quantity())
                .orElseThrow(() -> new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                        "Tồn khả dụng tại Inventory thay thế không đủ để giữ"));
        BigDecimal totalAfter = alreadyReallocated.add(request.quantity());
        BigDecimal remainingAfter = shortage.shortageQuantity().subtract(totalAfter);
        String statusAfter = remainingAfter.signum() == 0 ? "REPLACEMENT_RESERVED" : "OPEN";
        LocalDateTime now = LocalDateTime.now();
        ReallocationRecord record = new ReallocationRecord(UUID.randomUUID().toString(), caseId,
                operationId, reallocationId, targetInventoryId, request.quantity(), totalAfter,
                remainingAfter, statusAfter, payloadHash, actor, updated.getVersion(), now);

        if (!shortageRepository.insertReallocation(record)) {
            ReallocationRecord duplicate = shortageRepository.findReallocation(caseId, operationId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT,
                            "reallocationId hoặc operationId đã được dùng trong một thao tác khác"));
            return replayOrReject(duplicate, payloadHash);
        }
        if (!statusAfter.equals(shortage.status())
                && !shortageRepository.updateShortageStatus(caseId, shortage.status(), statusAfter)) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Shortage case vừa thay đổi; thao tác giữ thay thế đã được hoàn tác");
        }

        // Cùng transaction với balance: outbox publish được xử lý ở giai đoạn tích hợp Kafka.
        stockMovementRepository.insertMovement(StockMovement.builder()
                .id(UUID.randomUUID().toString())
                .inventoryId(targetInventoryId)
                .movementType(MovementType.RESERVE)
                .quantity(request.quantity())
                .physicalBefore(target.getPhysicalQuantity())
                .physicalAfter(target.getPhysicalQuantity())
                .reservedBefore(target.getReservedQuantity())
                .reservedAfter(updated.getReservedQuantity())
                .referenceType("INVENTORY_REALLOCATION")
                .referenceId(reallocationId)
                .reason("Giữ hàng thay thế cho shortage case " + caseId)
                .performedBy(actor)
                .performedBySubject(actor)
                .createdAt(now)
                .build());
        return record.toResponse();
    }

    private ShortageReallocationReservationResponse replayOrReject(ReallocationRecord existing,
                                                                    String payloadHash) {
        if (!MessageDigest.isEqual(existing.payloadHash().getBytes(StandardCharsets.US_ASCII),
                payloadHash.getBytes(StandardCharsets.US_ASCII))) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "operationId đã được dùng cho lệnh giữ thay thế với dữ liệu khác");
        }
        return existing.toResponse();
    }

    private String payloadHash(String caseId, String operationId, String reallocationId,
                               String targetInventoryId, BigDecimal quantity, long expectedVersion) {
        String canonical = String.join("\n", caseId, operationId, reallocationId, targetInventoryId,
                quantity.stripTrailingZeros().toPlainString(), Long.toString(expectedVersion));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không có sẵn trong JVM", exception);
        }
    }
}
