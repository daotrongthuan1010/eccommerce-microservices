package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateInventoryTransferRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ApproveInventoryTransferRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.DispatchInventoryTransferRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReceiveInventoryTransferRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryTransferResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryTransferDispatchResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryTransferReceiveResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.Inventory;
import com.dtthuan3.ecommerce.inventoryservice.entity.MovementType;
import com.dtthuan3.ecommerce.inventoryservice.entity.StockMovement;
import com.dtthuan3.ecommerce.inventoryservice.integration.event.InventoryTransferDiscrepancyEvent;
import com.dtthuan3.ecommerce.inventoryservice.integration.event.InventoryTransferReceivedEvent;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryEventOutboxRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryEventOutboxRepository.OutboxMessage;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryRepository.TransferDispatchUpdate;
import com.dtthuan3.ecommerce.inventoryservice.repository.StockMovementRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryTransferRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryTransferRepository.ReceiptProgressUpdate;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryTransferRepository.TransferHeaderRecord;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryTransferRepository.ApprovalRecord;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryTransferRepository.TransferLineRecord;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryTransferRepository.TransferReceiptOperationLineRecord;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryTransferRepository.TransferReceiptOperationRecord;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryLocationAccessService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryLocationAccessService.Position;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryTransferService;
import com.dtthuan3.ecommerce.inventoryservice.port.ActiveSkuValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;

/** Điều phối tạo phiếu điều chuyển nháp, xác thực phụ thuộc và ghi header/lines nguyên tử. */
@Service
@RequiredArgsConstructor
public class InventoryTransferServiceImpl implements InventoryTransferService {
    private final InventoryTransferRepository transferRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryLocationAccessService locationAccessService;
    private final ActiveSkuValidator skuValidator;
    private final ObjectMapper objectMapper;
    private final InventoryEventOutboxRepository eventOutboxRepository;
    private final TransactionTemplate transactionTemplate;
    private final DeadlockRetryExecutor deadlockRetryExecutor;

    @Value("${inventory.kafka.transfer-events-topic:inventory-transfer-events}")
    private String transferEventsTopic;

    /** Đọc snapshot header và các dòng hiện tại để client dùng đúng trạng thái/version mới nhất. */
    @Override
    @Transactional(readOnly = true)
    public InventoryTransferResponse getById(String transferId) {
        TransferHeaderRecord header = transferRepository.findById(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy phiếu điều chuyển: " + transferId));
        return toResponse(header, transferRepository.findLines(transferId));
    }

    /** Lấy trang phiếu theo doanh nghiệp; dữ liệu dòng được truy vấn theo batch ID để hạn chế N+1. */
    @Override
    @Transactional(readOnly = true)
    public Page<InventoryTransferResponse> list(String enterpriseId, String status, Pageable pageable) {
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim().toUpperCase();
        return transferRepository.search(enterpriseId.trim(), normalizedStatus, pageable)
                .map(header -> toResponse(header, transferRepository.findLines(header.id())));
    }

    /**
     * Retry được xử lý trước khi gọi Product Service; kiểm tra SKU chạy ngoài transaction DB.
     * Sau đó một transaction ngắn khóa hai vị trí, claim operationId và ghi cả phiếu lẫn dòng hàng.
     */
    @Override
    public InventoryTransferResponse create(CreateInventoryTransferRequest request, String createdBy) {
        NormalizedRequest normalized = normalize(request);
        String payloadHash = calculatePayloadHash(normalized);

        TransferHeaderRecord previous = transferRepository.findByOperationId(normalized.operationId()).orElse(null);
        if (previous != null) {
            return replayOrReject(previous, payloadHash);
        }

        // Xác minh SKU khi chưa mở transaction ghi để không giữ khóa database qua lời gọi service khác.
        skuValidator.requireActiveSkus(normalized.lines().stream().map(NormalizedLine::sku).toList());

        return transactionTemplate.execute(status -> createInsideTransaction(normalized, payloadHash, createdBy));
    }

    /** Duyệt tách người lập và người duyệt; khóa header để hai request không thể duyệt/dispatch cùng lúc. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryTransferResponse approve(String transferId, ApproveInventoryTransferRequest request,
                                             String approvedBy) {
        String actor = approvedBy == null ? "" : approvedBy.trim();
        String operationId = request.operationId().trim();
        String reason = request.reason() == null ? "" : request.reason().trim();
        String payloadHash = sha256(String.join("\n", transferId.trim(), operationId,
                request.expectedVersion().toString(), reason));

        TransferHeaderRecord header = transferRepository.findByIdForUpdate(transferId.trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy phiếu điều chuyển: " + transferId));

        // Nếu phản hồi lần đầu bị mất, cùng operation và payload trả lại trạng thái đã duyệt.
        ApprovalRecord previousApproval = transferRepository.findApproval(header.id()).orElse(null);
        if (previousApproval != null) {
            if (previousApproval.operationId().equals(operationId)
                    && MessageDigest.isEqual(previousApproval.payloadHash().getBytes(StandardCharsets.US_ASCII),
                    payloadHash.getBytes(StandardCharsets.US_ASCII))) {
                return toResponse(header, transferRepository.findLines(header.id()));
            }
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Phiếu đã được duyệt bằng thao tác khác; không thể duyệt lại");
        }
        if (!"CREATED".equals(header.status())) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Chỉ được duyệt phiếu điều chuyển ở trạng thái CREATED");
        }
        if (header.version() != request.expectedVersion()) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiếu điều chuyển đã thay đổi; hãy tải lại version mới nhất");
        }
        if (actor.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "Không xác định được người duyệt từ thông tin xác thực");
        }
        if (actor.equalsIgnoreCase(header.createdBy())) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "Người tạo phiếu không được tự duyệt phiếu của mình");
        }

        Instant approvedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        if (!transferRepository.approve(header.id(), request.expectedVersion(), operationId,
                payloadHash, actor, approvedAt)) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiếu vừa được cập nhật bởi thao tác khác; hãy tải lại trạng thái mới nhất");
        }
        TransferHeaderRecord approved = transferRepository.findById(header.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "Không đọc lại được phiếu sau khi duyệt"));
        return toResponse(approved, transferRepository.findLines(header.id()));
    }

    /**
     * Xuất toàn bộ phiếu trong một transaction cục bộ. Header được khóa trước, các dòng tồn được
     * khóa theo ID tăng dần, rồi một câu UPDATE theo lô trừ physical và tăng in-transit có điều kiện.
     */
    @Override
    public InventoryTransferDispatchResponse dispatch(String transferId,
                                                       DispatchInventoryTransferRequest request,
                                                       String dispatchedBy) {
        return deadlockRetryExecutor.execute("dispatch điều chuyển " + transferId,
                () -> dispatchInTransaction(transferId, request, dispatchedBy));
    }

    private InventoryTransferDispatchResponse dispatchInTransaction(String transferId,
                                                                      DispatchInventoryTransferRequest request,
                                                                      String dispatchedBy) {
        String operationId = request.operationId().trim();
        TransferHeaderRecord header = transferRepository.findByIdForUpdate(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy phiếu điều chuyển: " + transferId));

        // Cùng lệnh sau timeout trả lại snapshot đã lưu, kể cả khi trạng thái phiếu đã tiến xa hơn.
        if (header.dispatchOperationId() != null) {
            if (header.dispatchOperationId().equals(operationId)
                    && Objects.equals(header.dispatchExpectedVersion(), request.expectedVersion())) {
                return toDispatchResponse(header, transferRepository.findLines(transferId));
            }
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Phiếu đã được dispatch bằng operationId khác hoặc request retry không khớp");
        }
        if (!"CREATED".equals(header.status()) && !"APPROVED".equals(header.status())) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Chỉ được dispatch phiếu điều chuyển ở trạng thái CREATED hoặc APPROVED");
        }
        if (header.version() != request.expectedVersion()) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiếu điều chuyển đã thay đổi; hãy tải lại version mới nhất");
        }

        List<TransferLineRecord> lines = transferRepository.findLines(transferId);
        if (lines.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Không thể dispatch phiếu điều chuyển không có dòng hàng");
        }

        // Xác nhận hai đầu vẫn hoạt động; khóa SHARE được giữ trong transaction dispatch.
        locationAccessService.requireWritablePositions(List.of(
                new Position(header.enterpriseId(), header.sourceWarehouseId(), header.sourceLocationId()),
                new Position(header.enterpriseId(), header.destinationWarehouseId(), header.destinationLocationId())
        ));

        List<String> skus = lines.stream().map(TransferLineRecord::sku).toList();
        List<Inventory> lockedInventory = inventoryRepository.findByPositionForUpdate(
                header.enterpriseId(), header.sourceWarehouseId(), header.sourceLocationId(), skus);
        Map<String, Inventory> inventoryBySku = lockedInventory.stream()
                .collect(Collectors.toMap(Inventory::getSku, Function.identity()));

        // Kiểm tra đủ tất cả SKU trước khi cập nhật; dispatch nhiều dòng là all-or-nothing.
        List<TransferDispatchUpdate> updates = new ArrayList<>(lines.size());
        for (TransferLineRecord line : lines) {
            Inventory before = inventoryBySku.get(line.sku());
            if (before == null) {
                throw insufficientStock(line.sku(), line.plannedQuantity());
            }
            BigDecimal available = before.getPhysicalQuantity()
                    .subtract(before.getReservedQuantity())
                    .subtract(before.getDefectiveQuantity())
                    .subtract(before.getPendingQuantity());
            if (available.compareTo(line.plannedQuantity()) < 0) {
                throw insufficientStock(line.sku(), line.plannedQuantity());
            }
            updates.add(new TransferDispatchUpdate(before.getId(), line.plannedQuantity()));
        }

        // UPDATE ... FROM (VALUES ...) trừ/cộng theo lô và tự kiểm tra available tại database.
        List<Inventory> updatedInventory = inventoryRepository.dispatchTransferBatch(updates);
        if (updatedInventory.size() != lines.size()) {
            // Một điều kiện tồn không còn thỏa: exception làm rollback cả các dòng đã UPDATE trong lệnh.
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                    "Tồn khả dụng thay đổi trong lúc dispatch; không có dòng nào được xuất");
        }
        Map<String, Inventory> afterByInventoryId = updatedInventory.stream()
                .collect(Collectors.toMap(Inventory::getId, Function.identity()));

        List<StockMovement> movements = new ArrayList<>(lines.size());
        List<TransferLineRecord> dispatchedLines = new ArrayList<>(lines.size());
        for (TransferLineRecord line : lines) {
            Inventory before = inventoryBySku.get(line.sku());
            Inventory after = afterByInventoryId.get(before.getId());
            movements.add(StockMovement.builder()
                    .id(UUID.randomUUID().toString())
                    .inventoryId(after.getId())
                    .movementType(MovementType.TRANSFER_OUT)
                    .quantity(line.plannedQuantity())
                    .physicalBefore(before.getPhysicalQuantity())
                    .physicalAfter(after.getPhysicalQuantity())
                    .reservedBefore(before.getReservedQuantity())
                    .reservedAfter(after.getReservedQuantity())
                    .referenceType("INVENTORY_TRANSFER")
                    .referenceId(transferId)
                    .reason(header.reason())
                    .performedBy(dispatchedBy)
                    .performedBySubject(dispatchedBy)
                    .build());
            dispatchedLines.add(new TransferLineRecord(line.lineId(), line.lineNumber(), line.sku(),
                    line.plannedQuantity(), after.getId(), line.plannedQuantity(), after.getVersion(),
                    zeroIfNull(line.receivedGoodQuantity()), zeroIfNull(line.receivedDamagedQuantity()),
                    zeroIfNull(line.missingQuantity())));
        }

        // Movement, line audit, bucket tồn và trạng thái header cùng commit hoặc cùng rollback.
        stockMovementRepository.insertMovements(movements);
        if (!transferRepository.markLinesDispatched(transferId, dispatchedLines)) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Dòng phiếu đã thay đổi trong lúc dispatch; giao dịch đã được hoàn tác");
        }

        Instant dispatchedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        if (!transferRepository.markDispatched(transferId, request.expectedVersion(), operationId,
                dispatchedBy, dispatchedAt)) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiếu điều chuyển đã đổi trạng thái trong lúc dispatch; hãy tải lại dữ liệu");
        }

        TransferHeaderRecord dispatchedHeader = new TransferHeaderRecord(header.id(), header.enterpriseId(),
                header.sourceWarehouseId(), header.sourceLocationId(), header.destinationWarehouseId(),
                header.destinationLocationId(), header.operationId(), header.payloadHash(), header.referenceCode(),
                header.reason(), "IN_TRANSIT", header.version() + 1, header.createdBy(), header.createdAt(),
                operationId, request.expectedVersion(), dispatchedBy, dispatchedAt);
        return toDispatchResponse(dispatchedHeader, dispatchedLines);
    }

    /** Nhận hàng theo từng đợt; balance, tiến độ dòng, movement, outbox và trạng thái cùng commit. */
    @Override
    public InventoryTransferReceiveResponse receive(String transferId,
                                                     ReceiveInventoryTransferRequest request,
                                                     String receivedBy) {
        return deadlockRetryExecutor.execute("nhận điều chuyển " + transferId,
                () -> receiveInTransaction(transferId, request, receivedBy));
    }

    private InventoryTransferReceiveResponse receiveInTransaction(String transferId,
                                                                   ReceiveInventoryTransferRequest request,
                                                                   String receivedBy) {
        NormalizedReceiveRequest normalized = normalizeReceive(transferId, request);
        String payloadHash = calculateReceivePayloadHash(normalized, request.expectedVersion());

        // Khóa header để các đợt nhận của cùng phiếu được tuần tự hóa.
        TransferHeaderRecord header = transferRepository.findByIdForUpdate(transferId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy phiếu điều chuyển: " + transferId));

        // Retry được kiểm tra trước status/version vì phiếu có thể đã tiến tới RECEIVED sau lần đầu.
        TransferReceiptOperationRecord previous = transferRepository
                .findReceiptOperation(transferId, normalized.operationId()).orElse(null);
        if (previous != null) {
            if (!MessageDigest.isEqual(previous.payloadHash().getBytes(StandardCharsets.US_ASCII),
                    payloadHash.getBytes(StandardCharsets.US_ASCII))) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "operationId đã được dùng cho nội dung nhận điều chuyển khác");
            }
            return toReceiveResponse(previous,
                    transferRepository.findReceiptOperationLines(previous.id()));
        }

        if (!"IN_TRANSIT".equals(header.status()) && !"PARTIALLY_RECEIVED".equals(header.status())) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Chỉ được nhận phiếu điều chuyển đang IN_TRANSIT hoặc PARTIALLY_RECEIVED");
        }
        if (header.version() != request.expectedVersion()) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiếu điều chuyển đã thay đổi; hãy tải lại version mới nhất");
        }

        List<TransferLineRecord> allLines = transferRepository.findLines(transferId);
        Map<String, TransferLineRecord> linesById = allLines.stream()
                .collect(Collectors.toMap(TransferLineRecord::lineId, Function.identity()));
        List<ReceiptProgressUpdate> progressUpdates = new ArrayList<>(normalized.lines().size());

        // Tính lượng còn lại từ số liệu đã chốt; không cho nhận cùng một đơn vị hàng hai lần.
        for (NormalizedReceiptLine receiptLine : normalized.lines()) {
            TransferLineRecord transferLine = linesById.get(receiptLine.lineId());
            if (transferLine == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy lineId " + receiptLine.lineId() + " trong phiếu điều chuyển");
            }
            if (transferLine.sourceInventoryId() == null || transferLine.dispatchedQuantity() == null
                    || transferLine.dispatchedQuantity().signum() <= 0) {
                throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                        "Dòng " + receiptLine.lineId() + " chưa được dispatch");
            }

            BigDecimal goodBefore = zeroIfNull(transferLine.receivedGoodQuantity());
            BigDecimal damagedBefore = zeroIfNull(transferLine.receivedDamagedQuantity());
            BigDecimal missingBefore = zeroIfNull(transferLine.missingQuantity());
            BigDecimal accounted = receiptLine.goodQuantity().add(receiptLine.damagedQuantity())
                    .add(receiptLine.missingQuantity());
            BigDecimal remaining = transferLine.dispatchedQuantity()
                    .subtract(goodBefore).subtract(damagedBefore).subtract(missingBefore);
            if (accounted.compareTo(remaining) > 0) {
                throw new BusinessException(ErrorCode.INVALID_QUANTITY,
                        "Số lượng kiểm đếm vượt phần còn lại của dòng " + receiptLine.lineId()
                                + ": còn " + remaining);
            }

            progressUpdates.add(new ReceiptProgressUpdate(receiptLine.lineId(),
                    goodBefore.add(receiptLine.goodQuantity()),
                    damagedBefore.add(receiptLine.damagedQuantity()),
                    missingBefore.add(receiptLine.missingQuantity()), accounted));
        }

        // Khóa quyền ghi hai vị trí đến commit để kho không bị ngừng hoạt động giữa lúc cập nhật.
        locationAccessService.requireWritablePositions(List.of(
                new Position(header.enterpriseId(), header.sourceWarehouseId(), header.sourceLocationId()),
                new Position(header.enterpriseId(), header.destinationWarehouseId(), header.destinationLocationId())
        ));

        // Tạo trước balance đích chỉ cho những dòng thực nhận hàng; dòng thất lạc không tạo tồn 0.
        Map<String, String> destinationInventoryIds = new HashMap<>();
        for (NormalizedReceiptLine receiptLine : normalized.lines()) {
            if (receiptLine.goodQuantity().add(receiptLine.damagedQuantity()).signum() == 0) continue;
            TransferLineRecord transferLine = linesById.get(receiptLine.lineId());
            inventoryRepository.ensureExists(header.enterpriseId(), header.destinationWarehouseId(),
                    header.destinationLocationId(), transferLine.sku());
            Inventory destination = inventoryRepository.findByPosition(header.enterpriseId(),
                            header.destinationWarehouseId(), header.destinationLocationId(), transferLine.sku())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR,
                            "Không tìm thấy balance đích vừa được tạo cho SKU " + transferLine.sku()));
            destinationInventoryIds.put(receiptLine.lineId(), destination.getId());
        }

        // Khóa toàn bộ balance nguồn/đích cùng thứ tự ID để hai phiếu chuyển ngược chiều tránh deadlock.
        List<String> inventoryIdsToLock = new ArrayList<>();
        for (NormalizedReceiptLine receiptLine : normalized.lines()) {
            inventoryIdsToLock.add(linesById.get(receiptLine.lineId()).sourceInventoryId());
            String destinationId = destinationInventoryIds.get(receiptLine.lineId());
            if (destinationId != null) inventoryIdsToLock.add(destinationId);
        }
        Map<String, Inventory> lockedInventories = inventoryRepository.findByIdsForUpdate(inventoryIdsToLock)
                .stream().collect(Collectors.toMap(Inventory::getId, Function.identity()));

        List<StockMovement> movements = new ArrayList<>();
        List<TransferReceiptOperationLineRecord> operationLines = new ArrayList<>(normalized.lines().size());
        String receiptOperationId = UUID.randomUUID().toString();

        // Mỗi dòng trừ đúng lượng đã đối soát khỏi in-transit và đưa hàng tốt/hỏng vào kho đích.
        for (NormalizedReceiptLine receiptLine : normalized.lines()) {
            TransferLineRecord transferLine = linesById.get(receiptLine.lineId());
            BigDecimal accounted = receiptLine.goodQuantity().add(receiptLine.damagedQuantity())
                    .add(receiptLine.missingQuantity());
            Inventory sourceBefore = lockedInventories.get(transferLine.sourceInventoryId());
            if (sourceBefore == null || !matchesSource(sourceBefore, header, transferLine)) {
                throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                        "Dòng phiếu không còn trỏ đúng balance nguồn đã dispatch");
            }
            Inventory sourceAfter = inventoryRepository.decreaseInTransit(sourceBefore.getId(), accounted)
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                            "Bucket in-transit nguồn không đủ để đối soát dòng " + receiptLine.lineId()));

            BigDecimal physicallyReceived = receiptLine.goodQuantity().add(receiptLine.damagedQuantity());
            String destinationId = destinationInventoryIds.get(receiptLine.lineId());
            Inventory destinationAfter = null;
            if (physicallyReceived.signum() > 0) {
                Inventory destinationBefore = lockedInventories.get(destinationId);
                if (destinationBefore == null || !matchesDestination(destinationBefore, header, transferLine)) {
                    throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                            "Balance đích không khớp với SKU hoặc vị trí trên phiếu điều chuyển");
                }
                destinationAfter = inventoryRepository.receiveClassified(destinationBefore.getId(),
                        receiptLine.goodQuantity(), receiptLine.damagedQuantity(), BigDecimal.ZERO);
                movements.add(StockMovement.builder()
                        .id(UUID.randomUUID().toString())
                        .inventoryId(destinationAfter.getId())
                        .movementType(MovementType.TRANSFER_IN)
                        .quantity(physicallyReceived)
                        .physicalBefore(destinationBefore.getPhysicalQuantity())
                        .physicalAfter(destinationAfter.getPhysicalQuantity())
                        .reservedBefore(destinationBefore.getReservedQuantity())
                        .reservedAfter(destinationAfter.getReservedQuantity())
                        .referenceType("INVENTORY_TRANSFER")
                        .referenceId(transferId)
                        .reason(receiptLine.discrepancyReason() == null
                                ? header.reason() : receiptLine.discrepancyReason())
                        .performedBy(receivedBy)
                        .performedBySubject(receivedBy)
                        .build());
            }

            operationLines.add(new TransferReceiptOperationLineRecord(
                    UUID.randomUUID().toString(), receiptOperationId, transferLine.lineId(),
                    transferLine.sku(), receiptLine.goodQuantity(), receiptLine.damagedQuantity(),
                    receiptLine.missingQuantity(), receiptLine.discrepancyReason(), sourceAfter.getId(),
                    sourceAfter.getVersion(), destinationAfter == null ? null : destinationAfter.getId(),
                    destinationAfter == null ? null : destinationAfter.getVersion()));
        }

        if (!transferRepository.markLinesReceived(transferId, progressUpdates)) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Tiến độ nhận của một dòng đã thay đổi; lần nhận đã được hoàn tác");
        }

        Map<String, ReceiptProgressUpdate> progressByLineId = progressUpdates.stream()
                .collect(Collectors.toMap(ReceiptProgressUpdate::lineId, Function.identity()));
        boolean fullyAccounted = allLines.stream().allMatch(line -> {
            ReceiptProgressUpdate update = progressByLineId.get(line.lineId());
            BigDecimal good = update == null ? zeroIfNull(line.receivedGoodQuantity()) : update.receivedGoodQuantity();
            BigDecimal damaged = update == null ? zeroIfNull(line.receivedDamagedQuantity())
                    : update.receivedDamagedQuantity();
            BigDecimal missing = update == null ? zeroIfNull(line.missingQuantity()) : update.missingQuantity();
            return good.add(damaged).add(missing).compareTo(line.dispatchedQuantity()) == 0;
        });
        String statusAfter = fullyAccounted ? "RECEIVED" : "PARTIALLY_RECEIVED";
        if (!transferRepository.markReceived(transferId, request.expectedVersion(), statusAfter)) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiếu điều chuyển đã đổi trạng thái trong lúc nhận; lần nhận đã được hoàn tác");
        }

        TransferReceiptOperationRecord savedOperation = transferRepository.insertReceiptOperation(
                new TransferReceiptOperationRecord(receiptOperationId, transferId,
                        normalized.operationId(), payloadHash, statusAfter, header.version() + 1,
                        receivedBy, null));
        transferRepository.insertReceiptOperationLines(operationLines);
        if (!movements.isEmpty()) stockMovementRepository.insertMovements(movements);
        appendTransferReceiptEvents(savedOperation, operationLines);

        return toReceiveResponse(savedOperation, operationLines);
    }

    /** Thực hiện phần ghi cục bộ trong transaction để phiếu không thể tồn tại thiếu dòng hàng. */
    private InventoryTransferResponse createInsideTransaction(NormalizedRequest request, String payloadHash,
                                                               String createdBy) {
        // Giữ khóa SHARE đến commit để warehouse/location không thể bị ngừng hoạt động giữa kiểm tra và lưu.
        locationAccessService.requireWritablePositions(List.of(
                new Position(request.enterpriseId(), request.sourceWarehouseId(), request.sourceLocationId()),
                new Position(request.enterpriseId(), request.destinationWarehouseId(), request.destinationLocationId())
        ));

        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        TransferHeaderRecord candidate = new TransferHeaderRecord(
                UUID.randomUUID().toString(), request.enterpriseId(), request.sourceWarehouseId(),
                request.sourceLocationId(), request.destinationWarehouseId(), request.destinationLocationId(),
                request.operationId(), payloadHash, request.referenceCode(), request.reason(),
                "CREATED", 0L, createdBy, createdAt, null, null, null, null);

        // Unique index và INSERT ... ON CONFLICT bảo đảm chỉ một request thắng khi cùng operationId.
        if (!transferRepository.insertHeaderIfAbsent(candidate)) {
            TransferHeaderRecord winner = transferRepository.findByOperationId(request.operationId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR,
                            "Không tìm thấy phiếu điều chuyển sau khi tranh chấp operationId"));
            return replayOrReject(winner, payloadHash);
        }

        List<TransferLineRecord> lines = request.lines().stream()
                .map(line -> new TransferLineRecord(UUID.randomUUID().toString(), line.lineNumber(),
                        line.sku(), line.quantity(), null, BigDecimal.ZERO.setScale(4), null,
                        BigDecimal.ZERO.setScale(4), BigDecimal.ZERO.setScale(4), BigDecimal.ZERO.setScale(4)))
                .toList();
        transferRepository.insertLines(candidate.id(), lines);
        return toResponse(candidate, lines);
    }

    /** Cùng operationId và payload trả phiếu đã lưu; khác payload bị từ chối để không nhập nhằng chứng từ. */
    private InventoryTransferResponse replayOrReject(TransferHeaderRecord existing, String requestedHash) {
        if (!MessageDigest.isEqual(existing.payloadHash().getBytes(StandardCharsets.US_ASCII),
                requestedHash.getBytes(StandardCharsets.US_ASCII))) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "operationId đã được dùng cho nội dung phiếu điều chuyển khác");
        }
        return toResponse(existing, transferRepository.findLines(existing.id()));
    }

    /** Chuyển dữ liệu JDBC thành response công khai của API. */
    private InventoryTransferResponse toResponse(TransferHeaderRecord header, List<TransferLineRecord> lines) {
        List<InventoryTransferResponse.Line> responseLines = lines.stream()
                .map(line -> new InventoryTransferResponse.Line(line.lineId(), line.lineNumber(),
                        line.sku(), line.plannedQuantity()))
                .toList();
        return new InventoryTransferResponse(header.id(), header.operationId(), header.status(),
                header.enterpriseId(), header.sourceWarehouseId(), header.sourceLocationId(),
                header.destinationWarehouseId(), header.destinationLocationId(), header.referenceCode(),
                header.reason(), header.version(), header.createdBy(), header.createdAt(), responseLines);
    }

    /** Hash nội dung duyệt để operationId chỉ được dùng lại với đúng request ban đầu. */
    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không có sẵn trong JVM", exception);
        }
    }

    /** Dựng response ổn định từ header/line đã lưu, không đọc lại số dư có thể đã đổi về sau. */
    private InventoryTransferDispatchResponse toDispatchResponse(TransferHeaderRecord header,
                                                                  List<TransferLineRecord> lines) {
        List<InventoryTransferDispatchResponse.Line> responseLines = lines.stream()
                .map(line -> new InventoryTransferDispatchResponse.Line(line.lineId(), line.lineNumber(),
                        line.sku(), line.dispatchedQuantity(), line.sourceInventoryId(),
                        line.sourceInventoryVersionAfter()))
                .toList();
        return new InventoryTransferDispatchResponse(header.id(), header.status(),
                header.dispatchOperationId(), header.enterpriseId(), header.sourceWarehouseId(),
                header.sourceLocationId(), header.destinationWarehouseId(), header.destinationLocationId(),
                header.version(), header.dispatchedBy(), header.dispatchedAt(), responseLines);
    }

    /** Dựng response từ snapshot operation để retry không trả số dư hiện tại đã thay đổi về sau. */
    private InventoryTransferReceiveResponse toReceiveResponse(TransferReceiptOperationRecord operation,
                                                                List<TransferReceiptOperationLineRecord> lines) {
        List<InventoryTransferReceiveResponse.Line> responseLines = lines.stream()
                .map(line -> new InventoryTransferReceiveResponse.Line(line.transferLineId(), line.sku(),
                        line.goodQuantity(), line.damagedQuantity(), line.missingQuantity(),
                        line.discrepancyReason(), line.sourceInventoryId(), line.sourceInventoryVersionAfter(),
                        line.destinationInventoryId(), line.destinationInventoryVersionAfter()))
                .toList();
        boolean hasDiscrepancy = lines.stream().anyMatch(line -> line.missingQuantity().signum() > 0);
        return new InventoryTransferReceiveResponse(operation.transferId(), operation.operationId(),
                operation.statusAfter(), operation.transferVersionAfter(), operation.receivedBy(),
                operation.receivedAt(), hasDiscrepancy, responseLines);
    }

    /** Ghi event nghiệp vụ nhận/sai lệch vào outbox; publisher gửi sau khi transaction commit. */
    private void appendTransferReceiptEvents(TransferReceiptOperationRecord operation,
                                             List<TransferReceiptOperationLineRecord> lines) {
        String receivedEventId = deterministicEventId(operation.transferId(), operation.operationId(), "received");
        InventoryTransferReceivedEvent receivedEvent = new InventoryTransferReceivedEvent(
                receivedEventId, "TransferReceived.v1", operation.receivedAt(), operation.transferId(),
                operation.operationId(), operation.statusAfter(), operation.transferVersionAfter(),
                lines.stream().map(line -> new InventoryTransferReceivedEvent.Line(
                        line.transferLineId(), line.sku(), line.goodQuantity(), line.damagedQuantity(),
                        line.missingQuantity(), line.discrepancyReason(), line.destinationInventoryId()))
                        .toList());
        List<OutboxMessage> messages = new ArrayList<>();
        messages.add(new OutboxMessage(receivedEventId, transferEventsTopic, receivedEvent.eventType(),
                operation.transferId(), serializeEvent(receivedEvent), 0));

        List<InventoryTransferDiscrepancyEvent.Discrepancy> discrepancies = lines.stream()
                .filter(line -> line.missingQuantity().signum() > 0)
                .map(line -> new InventoryTransferDiscrepancyEvent.Discrepancy(
                        line.transferLineId(), line.sku(), line.missingQuantity(), line.discrepancyReason()))
                .toList();
        if (!discrepancies.isEmpty()) {
            String discrepancyEventId = deterministicEventId(
                    operation.transferId(), operation.operationId(), "discrepancy");
            InventoryTransferDiscrepancyEvent discrepancyEvent = new InventoryTransferDiscrepancyEvent(
                    discrepancyEventId, "TransferDiscrepancy.v1", operation.receivedAt(),
                    operation.transferId(), operation.operationId(), discrepancies);
            messages.add(new OutboxMessage(discrepancyEventId, transferEventsTopic,
                    discrepancyEvent.eventType(), operation.transferId(), serializeEvent(discrepancyEvent), 0));
        }
        eventOutboxRepository.appendBatch(messages);
    }

    /** Tạo event ID ổn định để thao tác retry không sinh thêm event nghiệp vụ. */
    private String deterministicEventId(String transferId, String operationId, String eventKind) {
        String value = "inventory-transfer:" + eventKind + ":" + transferId + ":" + operationId;
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** Tuần tự hóa payload trước commit để lỗi schema không làm mất sự kiện outbox. */
    private String serializeEvent(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Không thể tạo payload sự kiện điều chuyển");
        }
    }

    /** So khớp balance nguồn với snapshot SKU/vị trí đã ghi lúc dispatch. */
    private boolean matchesSource(Inventory inventory, TransferHeaderRecord header, TransferLineRecord line) {
        return Objects.equals(inventory.getEnterpriseId(), header.enterpriseId())
                && Objects.equals(inventory.getWarehouseId(), header.sourceWarehouseId())
                && Objects.equals(inventory.getLocationId(), header.sourceLocationId())
                && Objects.equals(inventory.getSku(), line.sku());
    }

    /** So khớp balance đích với SKU và vị trí nhận trên phiếu. */
    private boolean matchesDestination(Inventory inventory, TransferHeaderRecord header, TransferLineRecord line) {
        return Objects.equals(inventory.getEnterpriseId(), header.enterpriseId())
                && Objects.equals(inventory.getWarehouseId(), header.destinationWarehouseId())
                && Objects.equals(inventory.getLocationId(), header.destinationLocationId())
                && Objects.equals(inventory.getSku(), line.sku());
    }

    /** Các cột tiến độ có default 0; chuẩn hóa null từ dữ liệu cũ để tránh phép cộng null. */
    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** Lỗi số dư được trả cùng SKU và lượng dự kiến để nhân viên có thể kiểm tra lại nguồn. */
    private BusinessException insufficientStock(String sku, BigDecimal requestedQuantity) {
        return new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                "Tồn khả dụng không đủ để dispatch SKU " + sku + " với số lượng " + requestedQuantity);
    }

    /** Chuẩn hóa dòng nhận trước khi kiểm tra và tạo fingerprint idempotency. */
    private NormalizedReceiveRequest normalizeReceive(String transferId,
                                                      ReceiveInventoryTransferRequest request) {
        Set<String> seenLineIds = new HashSet<>();
        List<NormalizedReceiptLine> lines = new ArrayList<>(request.lines().size());
        for (ReceiveInventoryTransferRequest.Line line : request.lines()) {
            String lineId = trim(line.lineId());
            if (!seenLineIds.add(lineId)) {
                throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                        "lineId bị lặp trong request nhận hàng: " + lineId);
            }
            BigDecimal good = line.goodQuantity().setScale(4);
            BigDecimal damaged = line.damagedQuantity().setScale(4);
            BigDecimal missing = line.missingQuantity().setScale(4);
            if (good.signum() < 0 || damaged.signum() < 0 || missing.signum() < 0) {
                throw new BusinessException(ErrorCode.INVALID_QUANTITY,
                        "Số lượng nhận không được âm");
            }
            if (good.add(damaged).add(missing).signum() <= 0) {
                throw new BusinessException(ErrorCode.INVALID_QUANTITY,
                        "Mỗi dòng nhận phải có lượng tốt, hỏng hoặc thất lạc lớn hơn 0");
            }
            String discrepancyReason = trimToNull(line.discrepancyReason());
            if (missing.signum() > 0 && discrepancyReason == null) {
                throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                        "Phải ghi discrepancyReason khi khai báo hàng thất lạc");
            }
            lines.add(new NormalizedReceiptLine(lineId, good, damaged, missing, discrepancyReason));
        }
        lines.sort(Comparator.comparing(NormalizedReceiptLine::lineId));
        return new NormalizedReceiveRequest(transferId, trim(request.operationId()), List.copyOf(lines));
    }

    /** Hash payload nhận đã chuẩn hóa để cùng operationId không thể mang số lượng khác. */
    private String calculateReceivePayloadHash(NormalizedReceiveRequest request, Long expectedVersion) {
        try {
            List<HashReceiptLine> lines = request.lines().stream()
                    .map(line -> new HashReceiptLine(line.lineId(), line.goodQuantity().stripTrailingZeros(),
                            line.damagedQuantity().stripTrailingZeros(), line.missingQuantity().stripTrailingZeros(),
                            line.discrepancyReason()))
                    .toList();
            byte[] payload = objectMapper.writeValueAsBytes(
                    new HashReceivePayload(request.transferId(), expectedVersion, lines));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Không thể tạo mã xác thực payload nhận điều chuyển");
        }
    }

    /** Chuẩn hóa request trước khi tính fingerprint và gọi dịch vụ phụ thuộc. */
    private NormalizedRequest normalize(CreateInventoryTransferRequest request) {
        String sourceWarehouseId = trim(request.sourceWarehouseId());
        String sourceLocationId = trim(request.sourceLocationId());
        String destinationWarehouseId = trim(request.destinationWarehouseId());
        String destinationLocationId = trim(request.destinationLocationId());
        if (sourceWarehouseId.equals(destinationWarehouseId) && sourceLocationId.equals(destinationLocationId)) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "Vị trí nguồn và vị trí đích phải khác nhau");
        }

        Set<String> seenSkus = new HashSet<>();
        List<NormalizedLine> lines = new ArrayList<>(request.lines().size());
        for (int index = 0; index < request.lines().size(); index++) {
            CreateInventoryTransferRequest.Line line = request.lines().get(index);
            String sku = trim(line.sku());
            if (!seenSkus.add(sku)) {
                throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                        "SKU bị lặp trong phiếu điều chuyển: " + sku);
            }
            lines.add(new NormalizedLine(index + 1, sku, line.quantity().setScale(4)));
        }

        return new NormalizedRequest(trim(request.operationId()), trim(request.enterpriseId()),
                sourceWarehouseId, sourceLocationId, destinationWarehouseId, destinationLocationId,
                trimToNull(request.referenceCode()), trimToNull(request.reason()), List.copyOf(lines));
    }

    /** Tạo SHA-256 từ dữ liệu đã chuẩn hóa; 1.0 và 1.0000 được xem là cùng một request. */
    private String calculatePayloadHash(NormalizedRequest request) {
        try {
            List<HashLine> lines = request.lines().stream()
                    .map(line -> new HashLine(line.lineNumber(), line.sku(), line.quantity().stripTrailingZeros()))
                    .toList();
            byte[] payload = objectMapper.writeValueAsBytes(new HashPayload(request.enterpriseId(),
                    request.sourceWarehouseId(), request.sourceLocationId(), request.destinationWarehouseId(),
                    request.destinationLocationId(), request.referenceCode(), request.reason(), lines));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Không thể tạo mã xác thực payload phiếu điều chuyển");
        }
    }

    /** Chuẩn hóa chuỗi bắt buộc để so sánh và lưu dữ liệu nhất quán. */
    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    /** Chuỗi tùy chọn rỗng được lưu thành SQL NULL. */
    private String trimToNull(String value) {
        String normalized = trim(value);
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    private record NormalizedRequest(String operationId, String enterpriseId,
                                     String sourceWarehouseId, String sourceLocationId,
                                     String destinationWarehouseId, String destinationLocationId,
                                     String referenceCode, String reason, List<NormalizedLine> lines) {}

    private record NormalizedLine(int lineNumber, String sku, BigDecimal quantity) {}

    private record NormalizedReceiveRequest(String transferId, String operationId,
                                            List<NormalizedReceiptLine> lines) {}

    private record NormalizedReceiptLine(String lineId, BigDecimal goodQuantity,
                                         BigDecimal damagedQuantity, BigDecimal missingQuantity,
                                         String discrepancyReason) {}

    private record HashPayload(String enterpriseId, String sourceWarehouseId, String sourceLocationId,
                               String destinationWarehouseId, String destinationLocationId,
                               String referenceCode, String reason, List<HashLine> lines) {}

    private record HashLine(int lineNumber, String sku, BigDecimal quantity) {}

    private record HashReceivePayload(String transferId, Long expectedVersion,
                                     List<HashReceiptLine> lines) {}

    private record HashReceiptLine(String lineId, BigDecimal goodQuantity,
                                   BigDecimal damagedQuantity, BigDecimal missingQuantity,
                                   String discrepancyReason) {}
}
