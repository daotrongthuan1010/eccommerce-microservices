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
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryStockOperationRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.StockMovementRepository;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryLocationAccessService;
import com.dtthuan3.ecommerce.inventoryservice.service.ClassifiedStockReceipt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HexFormat;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Chứa nghiệp vụ tồn kho; cập nhật số lượng và lịch sử được xác nhận hoặc hoàn tác cùng nhau.
 * Class này hiện thực use case hoặc tác vụ nền của Inventory, phối hợp kiểm tra nghiệp vụ, repository và
 * tích hợp cần thiết để hoàn tất một bước xử lý; adapter bên ngoài không tự thao tác dữ liệu.
 */
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryBatchProperties batchProperties;
    private final InventoryLocationAccessService locationAccessService;
    private final DeadlockRetryExecutor deadlockRetryExecutor;
    private final InventoryAvailabilityReadCache availabilityReadCache;
    private final InventoryStockOperationRepository stockOperationRepository;
    private final ObjectMapper objectMapper;

    @Value("${inventory.api.idempotency-key-max-length}")
    private int idempotencyKeyMaxLength;

    /** Tìm hoặc tạo vị trí tồn theo SKU, cộng hàng nhập và ghi lịch sử trong một giao dịch. */
    @Override
    @Transactional
    public InventoryResponse receiveStock(
            ReceiveStockRequest request,
            String performedBy,
            String operationId
    ) {
        // Kiểm tra quyền vị trí trước khi trả kết quả retry, tránh khóa cũ làm bỏ qua quyền theo kho.
        locationAccessService.requireWritablePosition(
                request.enterpriseId(), request.warehouseId(), request.locationId());
        return runIdempotentStockOperation(operationId, "RECEIVE_STOCK", request, null, () -> {
            // INSERT ... ON CONFLICT khóa và trả dòng hiện tại, tránh đua nhau tạo cùng vị trí tồn.
            Inventory before = inventoryRepository.createOrFind(
                    request.enterpriseId(), request.warehouseId(), request.locationId(), request.sku());
            verifyVersion(before, request.expectedVersion());
            // Cập nhật số lượng và ghi lịch sử trong cùng giao dịch để hai phần dữ liệu luôn khớp nhau.
            Inventory updated = inventoryRepository.receive(before.getId(), request.quantity());
            recordMovement(before, updated, MovementType.RECEIVE, request.quantity(), request.referenceType(),
                    request.referenceId(), request.reason(), performedBy);
            return toResponse(updated);
        });
    }

    /**
     * Ghi các dòng phiếu nhập vào tồn và movement trong transaction đang xử lý phiếu.
     * Nếu API ghi sổ phiếu gặp lỗi ở bất kỳ dòng nào, transaction bên ngoài sẽ rollback cả loạt.
     */
    @Override
    @Transactional
    public void receiveClassifiedStock(List<ClassifiedStockReceipt> requests) {
        if (requests == null || requests.isEmpty()) return;

        // Kiểm tra và khóa toàn bộ Warehouse/Location theo batch, tránh một truy vấn cho mỗi dòng phiếu.
        locationAccessService.requireWritablePositions(requests.stream()
                .map(request -> new InventoryLocationAccessService.Position(
                        request.enterpriseId(), request.warehouseId(), request.locationId()))
                .distinct().toList());

        // Gộp các dòng cùng enterprise/kho/location/SKU để chỉ cập nhật một balance cho mỗi vị trí.
        Map<ClassifiedStockKey, ClassifiedStockReceipt> grouped = new HashMap<>();
        for (ClassifiedStockReceipt request : requests) {
            ClassifiedStockKey key = new ClassifiedStockKey(request.enterpriseId(), request.warehouseId(),
                    request.locationId(), request.sku(), request.receiptId());
            grouped.merge(key, request, (current, incoming) -> new ClassifiedStockReceipt(
                    current.enterpriseId(), current.warehouseId(), current.locationId(), current.sku(),
                    current.goodQuantity().add(incoming.goodQuantity()),
                    current.damagedQuantity().add(incoming.damagedQuantity()),
                    current.inspectionQuantity().add(incoming.inspectionQuantity()),
                    current.receiptId(), current.performedBy()));
        }

        // Khóa balance theo thứ tự cố định để hai phiếu nhiều SKU không khóa chéo nhau.
        List<ClassifiedStockReceipt> ordered = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ClassifiedStockKey::enterpriseId)
                        .thenComparing(ClassifiedStockKey::warehouseId)
                        .thenComparing(ClassifiedStockKey::locationId)
                        .thenComparing(ClassifiedStockKey::sku)
                        .thenComparing(ClassifiedStockKey::receiptId)))
                .map(Map.Entry::getValue)
                .toList();

        // Tạo balance thiếu trước, nhưng chưa giữ khóa balance nào trong bước này.
        // Nhờ vậy toàn bộ tồn của batch có thể được khóa một lượt theo cùng thứ tự ID như transfer.
        for (ClassifiedStockReceipt request : ordered) {
            inventoryRepository.ensureExists(request.enterpriseId(), request.warehouseId(),
                    request.locationId(), request.sku());
        }

        Map<ClassifiedStockKey, String> inventoryIdByPosition = new HashMap<>();
        List<String> inventoryIds = new ArrayList<>(ordered.size());
        for (ClassifiedStockReceipt request : ordered) {
            ClassifiedStockKey key = new ClassifiedStockKey(request.enterpriseId(), request.warehouseId(),
                    request.locationId(), request.sku(), request.receiptId());
            Inventory inventory = inventoryRepository.findByPosition(request.enterpriseId(),
                            request.warehouseId(), request.locationId(), request.sku())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR,
                            "Không tìm thấy balance vừa được khởi tạo cho SKU " + request.sku()));
            inventoryIdByPosition.put(key, inventory.getId());
            inventoryIds.add(inventory.getId());
        }
        Map<String, Inventory> lockedById = inventoryRepository.findByIdsForUpdate(inventoryIds).stream()
                .collect(Collectors.toMap(Inventory::getId, Function.identity()));

        List<StockMovement> movements = new ArrayList<>();
        for (ClassifiedStockReceipt request : ordered) {
            BigDecimal totalReceived = request.goodQuantity()
                    .add(request.damagedQuantity()).add(request.inspectionQuantity());
            if (totalReceived.signum() == 0) continue;

            ClassifiedStockKey key = new ClassifiedStockKey(request.enterpriseId(), request.warehouseId(),
                    request.locationId(), request.sku(), request.receiptId());
            Inventory before = lockedById.get(inventoryIdByPosition.get(key));
            if (before == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "Không khóa được balance của SKU " + request.sku());
            }
            Inventory after = inventoryRepository.receiveClassified(before.getId(), request.goodQuantity(),
                    request.damagedQuantity(), request.inspectionQuantity());

            // Mỗi movement liên kết về receipt; batch ghi lịch sử sau khi đã cập nhật các balance.
            movements.add(buildMovement(before, after, MovementType.RECEIVE, totalReceived,
                    "SUPPLIER_RECEIPT", request.receiptId(), null, request.performedBy()));
        }
        if (!movements.isEmpty()) stockMovementRepository.insertMovements(movements);
    }

    /** Khóa gộp ổn định cho một bucket tồn trong cùng phiếu nhập. */
    private record ClassifiedStockKey(String enterpriseId, String warehouseId, String locationId,
                                      String sku, String receiptId) {}

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
            ReceiveInventoryRequest request, String operationId
    ) {
        // Xác nhận quyền theo kho trước khi tra lại snapshot idempotency của một thao tác cũ.
        locationAccessService.requireWritableInventory(requireInventory(request.inventoryId()));
        return runIdempotentStockOperation(operationId, "RECEIVE_EXISTING", request, request.inventoryId(), () -> {
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
        });
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
            IssueInventoryRequest request, String operationId
    ) {
        // Không để khóa idempotency biến thành đường đọc response bỏ qua quyền của warehouse hiện tại.
        locationAccessService.requireWritableInventory(requireInventory(request.inventoryId()));
        return runIdempotentStockOperation(operationId, "ISSUE", request, request.inventoryId(), () -> {
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
        });
    }

    /** Đặt tồn vật lý bằng kết quả kiểm kê nhưng không thấp hơn các lượng đã cam kết. */
    @Override
    @Transactional
    public InventoryResponse adjust(AdjustInventoryRequest request, String operationId) {
        // Kiểm tra quyền trước khi trả snapshot idempotency để retry không vượt quyền hiện tại của Warehouse.
        locationAccessService.requireWritableInventory(requireInventory(request.inventoryId()));
        return runIdempotentStockOperation(operationId, "ADJUST", request, request.inventoryId(), () -> {
            // Khóa dòng tồn và xác nhận version để hai lần kiểm kê đồng thời không ghi đè lẫn nhau.
            Inventory before = requireLocked(request.inventoryId());
            locationAccessService.requireWritableInventory(before);
            verifyVersion(before, request.expectedVersion());

            // Cập nhật tồn vật lý theo số lượng thực tế sau kiểm kê; SQL từ chối nếu thấp hơn lượng đã cam kết.
            Inventory inventory = inventoryRepository.adjust(request.inventoryId(), request.actualQuantity())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_QUANTITY,
                            "actualQuantity không được nhỏ hơn tổng reserved, defective và pending"));
            recordMovement(before, inventory, MovementType.ADJUSTMENT,
                    request.actualQuantity().subtract(before.getPhysicalQuantity()).abs(), null, null,
                    request.reason(), null);
            return toResponse(inventory);
        });
    }

    /**
     * Chuyển hàng giữa hai vị trí tồn trong một giao dịch.
     * Khóa theo thứ tự ID cố định để hai request chuyển ngược chiều không khóa chéo;
     * sau khi trừ nguồn, cộng đích và ghi cặp lịch sử dùng chung mã tham chiếu.
     */
    @Override
    public TransferInventoryResponse transfer(TransferInventoryRequest request) {
        return deadlockRetryExecutor.execute("chuyển tồn giữa hai vị trí",
                () -> transferInTransaction(request));
    }

    private TransferInventoryResponse transferInTransaction(TransferInventoryRequest request) {
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
        // Lượt xem tồn theo SKU có thể dùng snapshot Redis ngắn hạn; lệnh reserve vẫn kiểm tra DB nguyên tử.
        return availabilityReadCache.findAvailable(sku, warehouseId, locationId, pageable).toPage(pageable);
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

    /** Đọc Inventory không khóa để kiểm tra quyền trước khi tra cứu kết quả idempotency. */
    private Inventory requireInventory(String id) {
        return inventoryRepository.findInventoryRowById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy Inventory với ID: " + id));
    }

    /** So sánh version client đã đọc với version hiện tại để phát hiện ghi dựa trên dữ liệu cũ. */
    private void verifyVersion(Inventory inventory, Long expectedVersion) {
        // Chặn lệnh dựa trên snapshot cũ; repository vẫn áp dụng điều kiện số lượng nguyên tử để bảo vệ tranh chấp
        // sau bước đọc.
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

    /**
     * Chạy thao tác nhập/xuất tối đa một lần cho mỗi Idempotency-Key.
     * Dấu khóa, thay đổi balance, movement và response snapshot cùng transaction; request đồng thời dùng cùng
     * khóa sẽ được PostgreSQL phân xử tại unique constraint, còn retry trả snapshot đã lưu mà không cộng/trừ lại.
     */
    private InventoryResponse runIdempotentStockOperation(String operationId, String operationType,
                                                           Object request, String inventoryId,
                                                           Supplier<InventoryResponse> operation) {
        String normalizedOperationId = operationId == null ? null : operationId.trim();
        if (normalizedOperationId == null || normalizedOperationId.isBlank()
                || normalizedOperationId.length() > idempotencyKeyMaxLength) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Idempotency-Key là bắt buộc và không được vượt quá "
                            + idempotencyKeyMaxLength + " ký tự");
        }

        String payloadHash = stockOperationPayloadHash(operationType, request);
        boolean claimed = stockOperationRepository.tryStart(normalizedOperationId, operationType,
                inventoryId, payloadHash, LocalDateTime.now());
        if (!claimed) {
            var previous = stockOperationRepository.findByOperationId(normalizedOperationId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT,
                            "Operation đang được xử lý; hãy retry cùng Idempotency-Key"));
            if (!operationType.equals(previous.operationType()) || !payloadHash.equals(previous.payloadHash())) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "Idempotency-Key đã được dùng cho thao tác hoặc dữ liệu request khác");
            }
            if (previous.responseJson() == null) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "Operation chưa có kết quả hoàn tất; hãy retry sau với cùng Idempotency-Key");
            }
            try {
                return objectMapper.readValue(previous.responseJson(), InventoryResponse.class);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Không thể đọc snapshot response của operation "
                        + normalizedOperationId, exception);
            }
        }

        InventoryResponse response = operation.get();
        try {
            stockOperationRepository.complete(normalizedOperationId, response.id(),
                    objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không thể lưu snapshot response cho operation "
                    + normalizedOperationId, exception);
        }
        return response;
    }

    /** Tạo fingerprint ổn định theo loại thao tác và toàn bộ payload để phát hiện tái sử dụng khóa sai. */
    private String stockOperationPayloadHash(String operationType, Object request) {
        try {
            byte[] canonicalPayload = objectMapper.writeValueAsBytes(new StockOperationPayload(operationType, request));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonicalPayload));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Không thể chuẩn hóa request nhập/xuất để kiểm tra retry", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM không hỗ trợ SHA-256", exception);
        }
    }

    /** Wrapper JSON dùng để payload hash không thể lẫn giữa các loại nghiệp vụ khác nhau. */
    private record StockOperationPayload(String operationType, Object request) {}

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
                .build().withBalanceSnapshot(before, after);
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
