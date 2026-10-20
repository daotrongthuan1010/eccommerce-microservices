package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.InspectReturnReceiptRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ReturnInspectionResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.Inventory;
import com.dtthuan3.ecommerce.inventoryservice.entity.MovementType;
import com.dtthuan3.ecommerce.inventoryservice.entity.StockMovement;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReturnInspectionRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReturnInspectionRepository.InspectionRecord;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReturnReceiptRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReturnReceiptRepository.ReturnReceiptRecord;
import com.dtthuan3.ecommerce.inventoryservice.repository.StockMovementRepository;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryReturnInspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/** Phân loại parcel đã nhận, cập nhật bucket tồn và ghi audit trong cùng transaction. */
@Service
@RequiredArgsConstructor
public class InventoryReturnInspectionServiceImpl implements InventoryReturnInspectionService {

    private final InventoryReturnReceiptRepository receiptRepository;
    private final InventoryReturnInspectionRepository inspectionRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    /**
     * Chốt một lần kiểm tra cho toàn bộ parcel: hàng đạt chuẩn rời pending để thành khả dụng,
     * hàng hỏng chuyển sang defective. Khóa receipt tuần tự hóa request cùng parcel; nếu có lỗi
     * ở bước cập nhật tồn, ghi movement hoặc insert kết quả thì toàn bộ transaction được rollback.
     */
    @Override
    @Transactional
    public ReturnInspectionResponse inspect(String receiptId, InspectReturnReceiptRequest request,
                                            String inspectedBy) {
        // Khóa phiếu trước để hai request đồng thời không thể cùng phân loại một parcel.
        ReturnReceiptRecord receipt = receiptRepository.findReceiptByIdForUpdate(receiptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy biên nhận hàng trả: " + receiptId));

        // Retry đúng operation và đúng dữ liệu trả lại kết quả đã ghi, không cập nhật tồn lần nữa.
        InspectionRecord previous = inspectionRepository.findByReceiptId(receiptId).orElse(null);
        if (previous != null) {
            if (!sameRequest(previous, request)) {
                throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE,
                        "Parcel đã được kiểm tra bằng operationId hoặc kết quả khác");
            }
            return toResponse(previous);
        }

        BigDecimal inspectedQuantity = request.goodQuantity().add(request.damagedQuantity());
        if (inspectedQuantity.compareTo(receipt.receivedQuantity()) != 0) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY,
                    "goodQuantity + damagedQuantity phải bằng số lượng parcel đã nhận: "
                            + receipt.receivedQuantity());
        }

        // Khóa số dư và kiểm tra parcel vẫn trỏ đúng dòng tồn trước khi đổi bucket.
        Inventory before = inventoryRepository.findInventoryRowByIdForUpdate(receipt.inventoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy Inventory của biên nhận: " + receipt.inventoryId()));
        if (!before.getSku().equals(receipt.sku())
                || !before.getEnterpriseId().equals(receipt.enterpriseId())
                || !before.getWarehouseId().equals(receipt.warehouseId())
                || !before.getLocationId().equals(receipt.locationId())) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Inventory không khớp với SKU hoặc vị trí lưu trên biên nhận hàng trả");
        }

        Inventory after = inventoryRepository.inspectPendingReturn(
                        before.getId(), inspectedQuantity, request.damagedQuantity())
                .orElseThrow(() -> new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                        "Số lượng chờ kiểm tra trong Inventory không đủ để phân loại parcel"));

        // Movement vật lý không đổi vì parcel đã được nhận trước; movement này ghi nhận việc đổi bucket.
        stockMovementRepository.insertMovement(StockMovement.builder()
                .id(UUID.randomUUID().toString())
                .inventoryId(after.getId())
                .movementType(MovementType.RETURN_INSPECT)
                .quantity(inspectedQuantity)
                .physicalBefore(before.getPhysicalQuantity())
                .physicalAfter(after.getPhysicalQuantity())
                .reservedBefore(before.getReservedQuantity())
                .reservedAfter(after.getReservedQuantity())
                .referenceType("CUSTOMER_RETURN_INSPECTION")
                .referenceId(receipt.id())
                .reason(request.reason())
                .performedBy(inspectedBy)
                .performedBySubject(inspectedBy)
                .build());

        InspectionRecord saved = inspectionRepository.insert(new InspectionRecord(
                UUID.randomUUID().toString(), receipt.id(), request.operationId(), after.getId(), after.getSku(),
                receipt.receivedQuantity(), request.goodQuantity(), request.damagedQuantity(), after.getVersion(),
                inspectedBy, request.reason(), null));
        return toResponse(saved);
    }

    /** So sánh khóa retry và kết quả để tránh dùng lại operationId cho nội dung khác. */
    private boolean sameRequest(InspectionRecord saved, InspectReturnReceiptRequest request) {
        return saved.operationId().equals(request.operationId())
                && saved.goodQuantity().compareTo(request.goodQuantity()) == 0
                && saved.damagedQuantity().compareTo(request.damagedQuantity()) == 0
                && saved.reason().equals(request.reason());
    }

    /** Chuyển bản ghi kiểm tra đã lưu thành DTO phản hồi cho cả request đầu và request retry. */
    private ReturnInspectionResponse toResponse(InspectionRecord inspection) {
        return new ReturnInspectionResponse(inspection.id(), inspection.operationId(), inspection.receiptId(),
                inspection.inventoryId(), inspection.sku(), inspection.receivedQuantity(),
                inspection.goodQuantity(), inspection.damagedQuantity(), inspection.inventoryVersionAfter(),
                "INSPECTED", inspection.inspectedBy(), inspection.inspectedAt());
    }
}
