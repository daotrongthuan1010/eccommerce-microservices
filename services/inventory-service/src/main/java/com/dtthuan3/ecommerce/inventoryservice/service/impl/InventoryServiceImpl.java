package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;

import com.dtthuan3.ecommerce.inventoryservice.config.InventoryBatchProperties;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.AdjustInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.IssueInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReceiveInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReceiveStockRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReleaseInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReserveInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.TransferInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.StockMovementResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.TransferInventoryResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.Inventory;
import com.dtthuan3.ecommerce.inventoryservice.entity.MovementType;
import com.dtthuan3.ecommerce.inventoryservice.entity.StockMovement;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.StockMovementRepository;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryLocationAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Chứa nghiệp vụ tồn kho; cập nhật số lượng và lịch sử được xác nhận hoặc hoàn tác cùng nhau. */
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryBatchProperties batchProperties;
    private final InventoryLocationAccessService locationAccessService;

    /** Tìm hoặc tạo vị trí tồn theo SKU, cộng hàng nhập và ghi lịch sử trong một giao dịch. */
    @Override
    @Transactional
    public InventoryResponse receiveStock(
            ReceiveStockRequest request,
            String performedBy
    ) {
        locationAccessService.requireWritablePosition(
                request.enterpriseId(), request.warehouseId(), request.locationId());
        // INSERT ... ON CONFLICT khóa và trả dòng hiện tại, tránh đua nhau tạo cùng vị trí tồn.
        Inventory before = inventoryRepository.createOrFind(
                request.enterpriseId(),
                request.warehouseId(),
                request.locationId(),
                request.sku()
        );
        verifyVersion(before, request.expectedVersion());
        // Cập nhật số lượng và ghi lịch sử trong cùng giao dịch để hai phần dữ liệu luôn khớp nhau.
        Inventory updated = inventoryRepository.receive(before.getId(), request.quantity());
        recordMovement(before, updated, MovementType.RECEIVE, request.quantity(), request.referenceType(),
                request.referenceId(), request.reason(), performedBy);
        return toResponse(updated);
    }

    /** Đọc theo ID và dựng phản hồi gồm tồn khả dụng sau khi trừ các lượng không thể bán. */
    @Override
    public InventoryResponse getById(String id) {
        // Đọc vị trí tồn; nếu không tồn tại thì trả lỗi 404.
        Inventory inventory = inventoryRepository.findInventoryRowById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy Inventory với ID: " + id));

        return toResponse(inventory);
    }

    /** Tìm vị trí tồn theo đủ bộ khóa để tránh trả nhầm SKU ở kho khác. */
    @Override
    public InventoryResponse getByPosition(
            String enterpriseId,
            String warehouseId,
            String locationId,
            String sku
    ) {

        // Bước 1: tìm vị trí tồn khớp đủ bốn trường khóa.
        Inventory inventory = inventoryRepository
                .findByPosition(
                        enterpriseId,
                        warehouseId,
                        locationId,
                        sku
                )
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.NOT_FOUND,
                                "Không tìm thấy Inventory phù hợp với các thông tin đã cung cấp"
                        )
                );

        return toResponse(inventory);
    }

    /** Tạo vị trí tồn mới; ràng buộc duy nhất ở cơ sở dữ liệu bảo vệ khi có yêu cầu đồng thời. */
    @Override
    @Transactional
    public InventoryResponse create(CreateInventoryRequest request) {

        locationAccessService.requireWritablePosition(
                request.enterpriseId(), request.warehouseId(), request.locationId());

        // INSERT ... ON CONFLICT DO NOTHING tránh race condition và chỉ cần một round-trip.
        Inventory inventory = inventoryRepository.create(
                request.enterpriseId(),
                request.warehouseId(),
                request.locationId(),
                request.sku()
        ).orElseThrow(() -> new BusinessException(ErrorCode.DUPLICATE_RESOURCE,
                "Inventory cho SKU này đã tồn tại: " + request.sku()));

        return toResponse(inventory);
    }

    /** Tạo số lượng vị trí tồn theo cấu hình trong một JDBC batch, rollback toàn lô nếu có khóa trùng. */
    @Override
    @Transactional
    public List<InventoryResponse> createBatch(List<CreateInventoryRequest> requests) {
        if (requests == null || requests.isEmpty() || requests.size() > batchProperties.maxSize()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Batch phải có từ 1 đến " + batchProperties.maxSize() + " Inventory");
        }

        locationAccessService.requireWritablePositions(requests.stream()
                .map(request -> new InventoryLocationAccessService.Position(
                        request.enterpriseId(), request.warehouseId(), request.locationId()))
                .distinct()
                .toList());

        // Chuẩn bị ID trước khi gửi JDBC batch vì khóa chính String không dùng sequence database.
        List<Inventory> inventories = requests.stream()
                .map(request -> Inventory.builder()
                        .id(UUID.randomUUID().toString())
                        .enterpriseId(request.enterpriseId())
                        .warehouseId(request.warehouseId())
                        .locationId(request.locationId())
                        .sku(request.sku())
                        .build())
                .toList();

        // DO NOTHING giúp nhận biết trùng; ném lỗi trong transaction sẽ rollback cả những dòng đã chèn.
        int[] results = inventoryRepository.createBatch(inventories);
        for (int result : results) {
            if (result == 0) {
                throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Một Inventory trong Batch đã tồn tại");
            }
        }

        // Đọc lại một lượt để trả cả createdAt/updatedAt do database tạo, đồng thời giữ thứ tự request.
        Map<String, Inventory> createdById = new HashMap<>();
        inventoryRepository.findByIds(inventories.stream().map(Inventory::getId).toList())
                .forEach(inventory -> createdById.put(inventory.getId(), inventory));
        return inventories.stream()
                .map(inventory -> toResponse(createdById.get(inventory.getId())))
                .toList();
    }

/** Nhập thêm hàng vào vị trí đã tồn tại và ghi lịch sử loại RECEIVE. */
    @Override
    @Transactional
    public InventoryResponse receive(
            ReceiveInventoryRequest request
    ) {

        /*
         * Bước 1:
         *
         * Kiểm tra Inventory có tồn tại không.
         */
        Inventory before = requireLocked(request.inventoryId());
        locationAccessService.requireWritableInventory(before);
        verifyVersion(before, request.expectedVersion());

        /*
         * Bước 2:
         *
         * Tăng số lượng hàng vật lý.
         *
         * Ví dụ:
         *
         * Tồn vật lý = 100
         * Lượng nhập = 30
         *
         * Kết quả: tồn vật lý = 130
         */
        Inventory inventory = inventoryRepository.receive(
                request.inventoryId(),
                request.quantity()
        );
        recordMovement(before, inventory, MovementType.RECEIVE, request.quantity(), null, null, null, null);

        /*
         * Bước 3:
         *
         * Chuyển đối tượng tồn kho thành phản hồi.
         */
        return toResponse(inventory);
    }

    /** Giữ hàng nếu tồn khả dụng đủ; nếu không đủ thì không cập nhật và trả lỗi xung đột. */
    @Override
    @Transactional
    public InventoryResponse reserve(
            ReserveInventoryRequest request
    ) {

        /*
         * Bước 1:
         *
         * Kiểm tra Inventory có tồn tại không.
         */
        Inventory before = requireLocked(request.inventoryId());
        locationAccessService.requireWritableInventory(before);
        verifyVersion(before, request.expectedVersion());

        /*
         * Bước 2:
         *
         * Thử giữ lượng hàng yêu cầu.
         *
         * Repository chỉ UPDATE nếu:
         *
         * Chỉ cập nhật khi lượng khả dụng lớn hơn hoặc bằng lượng cần giữ.
         */
        Inventory inventory = inventoryRepository
                .reserve(
                        request.inventoryId(),
                        request.quantity()
                )
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                                "Tồn khả dụng không đủ cho Inventory có ID: "
                                        + request.inventoryId()
                        )
                );
        recordMovement(before, inventory, MovementType.RESERVE, request.quantity(), null, null, null, null);

        return toResponse(inventory);
    }

    /** Bỏ giữ hàng nếu lượng reserve hiện tại đủ lớn hơn hoặc bằng lượng yêu cầu. */
    @Override
    @Transactional
    public InventoryResponse release(
            ReleaseInventoryRequest request
    ) {

        /*
         * Bước 1:
         *
         * Kiểm tra Inventory có tồn tại không.
         */
        Inventory before = requireLocked(request.inventoryId());
        locationAccessService.requireWritableInventory(before);
        verifyVersion(before, request.expectedVersion());

        /*
         * Bước 2:
         *
         * Thực hiện bỏ giữ hàng.
         *
         * Repository chỉ UPDATE khi:
         *
         * Chỉ cập nhật khi lượng đã giữ đủ để bỏ giữ.
         */
        Inventory inventory = inventoryRepository
                .release(
                        request.inventoryId(),
                        request.quantity()
                )
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.INVALID_QUANTITY,
                                "Số lượng cần bỏ giữ không được lớn hơn số lượng đã reserve"
                        )
                );
        recordMovement(before, inventory, MovementType.RELEASE, request.quantity(), null, null, null, null);

        return toResponse(inventory);
    }

    /** Xuất hàng từ phần tồn khả dụng, không đụng đến hàng giữ/lỗi/đang chờ. */
    @Override
    @Transactional
    public InventoryResponse issue(
            IssueInventoryRequest request
    ) {

        /*
         * Bước 1:
         *
         * Kiểm tra Inventory có tồn tại không.
         */
        Inventory before = requireLocked(request.inventoryId());
        locationAccessService.requireWritableInventory(before);
        verifyVersion(before, request.expectedVersion());

        /*
         * Bước 2:
         *
         * Thực hiện xuất hàng.
         *
         * Repository chỉ UPDATE nếu:
         *
         * Chỉ cập nhật khi lượng khả dụng đủ để xuất.
         */
        Inventory inventory = inventoryRepository
                .issue(
                        request.inventoryId(),
                        request.quantity()
                )
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                                "Tồn khả dụng không đủ cho Inventory có ID: "
                                        + request.inventoryId()
                        )
                );
        recordMovement(before, inventory, MovementType.ISSUE, request.quantity(), null, null, null, null);

        return toResponse(inventory);
    }

    /** Đặt tồn vật lý bằng kết quả kiểm kê nhưng không thấp hơn các lượng đã cam kết. */
    @Override
    @Transactional
    public InventoryResponse adjust(
            AdjustInventoryRequest request
    ) {

        // Bước 1:
        // Kiểm tra Inventory có tồn tại hay không.
        Inventory before = requireLocked(request.inventoryId());
        locationAccessService.requireWritableInventory(before);
        verifyVersion(before, request.expectedVersion());

        // Bước 2:
        // Cập nhật tồn vật lý theo số lượng thực tế sau khi kiểm kê.
        Inventory inventory = inventoryRepository
                .adjust(
                        request.inventoryId(),
                        request.actualQuantity()
                )
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.INVALID_QUANTITY,
                                "actualQuantity không được nhỏ hơn tổng reserved, defective và pending"
                        )
                );
        recordMovement(before, inventory, MovementType.ADJUSTMENT,
                request.actualQuantity().subtract(before.getPhysicalQuantity()).abs(), null, null,
                request.reason(), null);

        return toResponse(inventory);
    }

    /**
     * Chuyển hàng giữa hai vị trí tồn trong một giao dịch.
     * Khóa theo thứ tự ID cố định để hai request chuyển ngược chiều không khóa chéo;
     * sau khi trừ nguồn, cộng đích và ghi cặp lịch sử dùng chung mã tham chiếu.
     */
    @Override
    @Transactional
    public TransferInventoryResponse transfer(TransferInventoryRequest request) {
        // Một dòng tồn không thể vừa là nguồn vừa là đích của cùng lần chuyển.
        if (request.fromInventoryId().equals(request.toInventoryId())) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "Inventory nguồn và Inventory đích phải khác nhau");
        }

        // Khóa ID theo thứ tự cố định để hạn chế deadlock khi có hai luồng chuyển ngược chiều.
        List<String> ids = new ArrayList<>(List.of(request.fromInventoryId(), request.toInventoryId()));
        ids.sort(String::compareTo);
        Inventory first = requireLocked(ids.get(0));
        Inventory second = requireLocked(ids.get(1));
        // Ghép lại vai trò nguồn/đích sau khi đã khóa theo thứ tự ID.
        Inventory sourceBefore = first.getId().equals(request.fromInventoryId()) ? first : second;
        Inventory destinationBefore = first.getId().equals(request.toInventoryId()) ? first : second;
        locationAccessService.requireWritableInventory(sourceBefore);
        locationAccessService.requireWritableInventory(destinationBefore);
        // Kiểm tra version client gửi và bảo đảm hai đầu thuộc cùng doanh nghiệp, cùng SKU.
        verifyVersion(sourceBefore, request.expectedSourceVersion());
        verifyVersion(destinationBefore, request.expectedDestinationVersion());
        if (!sourceBefore.getEnterpriseId().equals(destinationBefore.getEnterpriseId())
                || !sourceBefore.getSku().equals(destinationBefore.getSku())) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "Transfer phải nằm trong cùng enterprise và sử dụng cùng SKU");
        }

        // Trừ nguồn trước; nếu lượng khả dụng không đủ, ném lỗi để rollback giao dịch.
        Inventory sourceAfter = inventoryRepository.issue(sourceBefore.getId(), request.quantity())
                .orElseThrow(() -> new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                        "Tồn khả dụng không đủ để thực hiện Transfer"));
        // Chỉ cộng đích sau khi trừ nguồn thành công; cả hai câu lệnh nằm trong cùng giao dịch.
        Inventory destinationAfter = inventoryRepository.receive(destinationBefore.getId(), request.quantity());
        // Dùng chung một mã tham chiếu để truy ngược hai biến động xuất/nhập của lần chuyển này.
        String transferReference = request.referenceId() == null ? UUID.randomUUID().toString() : request.referenceId();
        String transferType = request.referenceType() == null ? "TRANSFER" : request.referenceType();
        // Ghi lịch sử của cả hai đầu trước khi commit để không chỉ lưu được một nửa giao dịch.
        stockMovementRepository.insertMovements(List.of(
                buildMovement(sourceBefore, sourceAfter, MovementType.TRANSFER_OUT, request.quantity(),
                        transferType, transferReference, request.reason(), null),
                buildMovement(destinationBefore, destinationAfter, MovementType.TRANSFER_IN, request.quantity(),
                        transferType, transferReference, request.reason(), null)
        ));
        return new TransferInventoryResponse(
                toResponse(sourceAfter),
                toResponse(destinationAfter),
                transferType,
                transferReference);
    }

    /** Tra vị trí tồn theo SKU; một SKU có thể nằm ở nhiều kho hoặc vị trí. */
    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> bySku(String sku, Pageable pageable) {
        return inventoryRepository.findBySku(sku, pageable).map(this::toResponse);
    }

    /** Tra các vị trí tồn thuộc một kho. */
    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> byWarehouse(String warehouseId, Pageable pageable) {
        return inventoryRepository.findByWarehouse(warehouseId, pageable).map(this::toResponse);
    }

    /** Tra các vị trí tồn thuộc một khu vực lưu trữ. */
    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> byLocation(String locationId, Pageable pageable) {
        return inventoryRepository.findByLocation(locationId, pageable).map(this::toResponse);
    }

    /** Tra các vị trí còn hàng có thể bán, có thể lọc thêm theo SKU/kho/vị trí. */
    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> available(String sku, String warehouseId, String locationId, Pageable pageable) {
        return inventoryRepository.findAvailable(sku, warehouseId, locationId, pageable)
                .map(this::toResponse);
    }

    /** Kiểm tra khoảng thời gian rồi chuyển bộ lọc và thông tin trang xuống truy vấn SQL. */
    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementResponse> movementHistory(String inventoryId, MovementType type,
                                                       String referenceId, LocalDateTime from,
                                                       LocalDateTime to, Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_TIME_RANGE,
                    "Thời điểm from phải trước hoặc bằng thời điểm to");
        }
        return stockMovementRepository.searchMovements(inventoryId, type, referenceId, from, to, pageable);
    }

    /** Lấy khóa ghi ở cơ sở dữ liệu; khóa được giữ đến khi giao dịch kết thúc. */
    private Inventory requireLocked(String id) {
        return inventoryRepository.findInventoryRowByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy Inventory với ID: " + id));
    }

    /** So sánh version client đã đọc với version hiện tại để phát hiện ghi dựa trên dữ liệu cũ. */
    private void verifyVersion(Inventory inventory, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(inventory.getVersion())) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Version Inventory đã cũ; hãy tải lại dữ liệu rồi thử lại");
        }
    }

    /** Chụp số lượng trước/sau và ghi lịch sử cùng giao dịch cập nhật tồn kho. */
    private void recordMovement(Inventory before, Inventory after, MovementType type, BigDecimal quantity,
                                String referenceType, String referenceId, String reason, String performedBy) {
        stockMovementRepository.insertMovement(buildMovement(
                before, after, type, quantity, referenceType, referenceId, reason, performedBy));
    }

    /** Dựng movement và giữ nguyên subject JWT dạng chuỗi để batch hoặc insert đơn lẻ dùng chung. */
    private StockMovement buildMovement(Inventory before, Inventory after, MovementType type, BigDecimal quantity,
                                        String referenceType, String referenceId, String reason, String performedBy) {
        String actor = performedBy;
        if (actor == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) actor = authentication.getName();
        }
        return StockMovement.builder()
                .id(UUID.randomUUID().toString())
                .inventoryId(after.getId())
                .movementType(type)
                .quantity(quantity)
                .physicalBefore(before.getPhysicalQuantity())
                .physicalAfter(after.getPhysicalQuantity())
                .reservedBefore(before.getReservedQuantity())
                .reservedAfter(after.getReservedQuantity())
                .referenceType(referenceType)
                .referenceId(referenceId)
                .reason(reason)
                .performedBy(actor)
                .performedBySubject(actor)
                .build();
    }

    /** Dùng chung phép tính tồn khả dụng để mọi endpoint trả về cùng một công thức. */
    private InventoryResponse toResponse(Inventory inventory) {
        BigDecimal availableQuantity = inventory.getPhysicalQuantity()
                .subtract(inventory.getReservedQuantity())
                .subtract(inventory.getDefectiveQuantity())
                .subtract(inventory.getPendingQuantity());
        return new InventoryResponse(inventory.getId(), inventory.getEnterpriseId(), inventory.getWarehouseId(),
                inventory.getLocationId(), inventory.getSku(), inventory.getPhysicalQuantity(),
                inventory.getReservedQuantity(), inventory.getDefectiveQuantity(), inventory.getPendingQuantity(),
                inventory.getInTransitQuantity(), availableQuantity, inventory.getVersion(),
                inventory.getCreatedAt(), inventory.getUpdatedAt());
    }

}
