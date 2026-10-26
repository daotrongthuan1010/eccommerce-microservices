package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReturnInspectionRepository;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReceiveReturnReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ReturnReceiptResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.Inventory;
import com.dtthuan3.ecommerce.inventoryservice.entity.MovementType;
import com.dtthuan3.ecommerce.inventoryservice.entity.StockMovement;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReturnReceiptRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReturnReceiptRepository.ReturnLineProgress;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReturnReceiptRepository.ReturnReceiptRecord;
import com.dtthuan3.ecommerce.inventoryservice.repository.StockMovementRepository;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryLocationAccessService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryReturnReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/** Nhận hàng trả đã duyệt vào khu cách ly và bảo đảm retry parcel không cộng tồn lần hai. */
@Service
@RequiredArgsConstructor
public class InventoryReturnReceiptServiceImpl implements InventoryReturnReceiptService {
    private final InventoryRepository inventoryRepository;
    private final InventoryReturnReceiptRepository returnReceiptRepository;
    private final InventoryReturnInspectionRepository returnInspectionRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryLocationAccessService locationAccessService;

    /** Tra cứu trạng thái hiện tại; trạng thái INSPECTED chỉ được trả khi đã có kết quả lưu bền vững. */
    @Override
    @Transactional(readOnly = true)
    public ReturnReceiptResponse getById(String receiptId) {
        ReturnReceiptRecord receipt = returnReceiptRepository.findReceiptById(receiptId.trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy biên nhận hàng trả: " + receiptId));
        String status = returnInspectionRepository.findByReceiptId(receiptId.trim()).isPresent()
                ? "INSPECTED" : "QUARANTINED";
        return toResponse(receipt, status);
    }

    @Override
    @Transactional
    public ReturnReceiptResponse receive(ReceiveReturnReceiptRequest request, String performedBy) {
        // Hạn mức được chụp từ ReturnRequest đã duyệt; khóa dòng cho đến hết transaction để chống nhận vượt.
        ReturnLineProgress progress = returnReceiptRepository.lockOrCreateProgress(
                request.returnRequestId(), request.orderLineId(), request.approvedQuantity());
        if (progress.approvedQuantity().compareTo(request.approvedQuantity()) != 0) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "approvedQuantity không khớp với hạn mức đã lưu cho dòng trả hàng");
        }

        ReturnReceiptRecord previous = returnReceiptRepository.findReceipt(
                request.returnRequestId(), request.orderLineId(), request.parcelIdentity()).orElse(null);
        if (previous != null) {
            if (!sameRequest(previous, request)) {
                throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE,
                        "Parcel này đã được nhận với nội dung khác");
            }
            return toResponse(previous);
        }

        locationAccessService.requireWritablePosition(
                request.enterpriseId(), request.warehouseId(), request.locationId());

        BigDecimal remaining = progress.approvedQuantity().subtract(progress.receivedQuantity());
        if (request.receivedQuantity().compareTo(remaining) > 0) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY,
                    "Tổng số lượng nhận vượt số lượng đã duyệt còn lại: " + remaining);
        }

        Inventory before = inventoryRepository.createOrFind(request.enterpriseId(), request.warehouseId(),
                request.locationId(), request.sku());
        // Hàng trả mới nhận luôn vào pending_quantity, không trở thành tồn bán được trước khi kiểm tra.
        Inventory after = inventoryRepository.receiveClassified(before.getId(), BigDecimal.ZERO,
                BigDecimal.ZERO, request.receivedQuantity());
        stockMovementRepository.insertMovement(StockMovement.builder()
                .id(UUID.randomUUID().toString())
                .inventoryId(after.getId())
                .movementType(MovementType.RETURN_RECEIVE)
                .quantity(request.receivedQuantity())
                .physicalBefore(before.getPhysicalQuantity())
                .physicalAfter(after.getPhysicalQuantity())
                .reservedBefore(before.getReservedQuantity())
                .reservedAfter(after.getReservedQuantity())
                .referenceType("CUSTOMER_RETURN")
                .referenceId(request.returnRequestId())
                .reason(request.reason())
                .performedBy(performedBy)
                .performedBySubject(performedBy)
                .build());

        BigDecimal lineReceivedAfter = progress.receivedQuantity().add(request.receivedQuantity());
        if (!returnReceiptRepository.addReceivedQuantity(request.returnRequestId(), request.orderLineId(),
                request.receivedQuantity())) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY,
                    "Tổng số lượng nhận đã vượt số lượng được duyệt");
        }

        ReturnReceiptRecord receipt = new ReturnReceiptRecord(
                UUID.randomUUID().toString(), request.returnRequestId(), request.orderLineId(),
                request.parcelIdentity(), after.getId(), request.enterpriseId(), request.warehouseId(),
                request.locationId(), request.sku(), request.approvedQuantity(), request.receivedQuantity(),
                lineReceivedAfter, after.getVersion(), performedBy, request.reason(), null);
        ReturnReceiptRecord saved = returnReceiptRepository.insertReceipt(receipt);
        return toResponse(saved);
    }

    private boolean sameRequest(ReturnReceiptRecord saved, ReceiveReturnReceiptRequest request) {
        return saved.enterpriseId().equals(request.enterpriseId())
                && saved.warehouseId().equals(request.warehouseId())
                && saved.locationId().equals(request.locationId())
                && saved.sku().equals(request.sku())
                && saved.approvedQuantity().compareTo(request.approvedQuantity()) == 0
                && saved.receivedQuantity().compareTo(request.receivedQuantity()) == 0
                && saved.reason().equals(request.reason());
    }

    private ReturnReceiptResponse toResponse(ReturnReceiptRecord record) {
        return toResponse(record, "QUARANTINED");
    }

    private ReturnReceiptResponse toResponse(ReturnReceiptRecord record, String status) {
        return new ReturnReceiptResponse(record.id(), record.returnRequestId(), record.orderLineId(),
                record.parcelIdentity(), record.inventoryId(), record.sku(), record.warehouseId(),
                record.locationId(), record.approvedQuantity(), record.receivedQuantity(),
                record.lineReceivedAfter(), record.inventoryVersionAfter(), status, record.createdAt());
    }
}
