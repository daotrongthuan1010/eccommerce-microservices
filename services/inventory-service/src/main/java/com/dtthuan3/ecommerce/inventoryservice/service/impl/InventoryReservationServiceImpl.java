package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CommitInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.DispatchInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReleaseInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryReservationResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.Inventory;
import com.dtthuan3.ecommerce.inventoryservice.entity.MovementType;
import com.dtthuan3.ecommerce.inventoryservice.entity.StockMovement;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReservationRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReservationRepository.ReservationHeaderRecord;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReservationRepository.ReservationLineRecord;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReservationRepository.ReservationOperationRecord;
import com.dtthuan3.ecommerce.inventoryservice.repository.StockMovementRepository;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryLocationAccessService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Điều phối reservation theo dòng với khóa ổn định, giao dịch cục bộ và retry deadlock giới hạn. */
@Service
@RequiredArgsConstructor
public class InventoryReservationServiceImpl implements InventoryReservationService {
    private final InventoryReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryLocationAccessService locationAccessService;
    private final StockMovementRepository movementRepository;
    private final DeadlockRetryExecutor deadlockRetryExecutor;

    @Override
    public InventoryReservationResponse getById(String reservationId) {
        ReservationHeaderRecord header = reservationRepository.findById(reservationId.trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy reservation: " + reservationId));
        return reservationRepository.toResponse(header, reservationRepository.findLines(header.id()));
    }

    /** Nhả tồn của reservation HELD đã quá hạn; endpoint này cũng có thể được scheduler gọi về sau. */
    @Override
    public InventoryReservationResponse expire(String reservationId, String expiredBy) {
        String id = reservationId.trim();
        String actor = actor(expiredBy);
        return deadlockRetryExecutor.execute("expire reservation " + id, () -> {
            ReservationHeaderRecord header = lockHeader(id);
            if ("EXPIRED".equals(header.status())) return current(id);
            if (!"HELD".equals(header.status())) {
                throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                        "Chỉ reservation HELD mới được expire; trạng thái hiện tại: " + header.status());
            }
            if (!reservationRepository.expireIfDue(id, header.version())) {
                throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                        "Chưa đến hạn expire hoặc reservation vừa được cập nhật");
            }
            List<LineQuantity> remainingLines = resolveReleaseLines(
                    reservationRepository.findLines(id), null);
            applyRelease(id, remainingLines, "Tự động nhả hàng do reservation hết hạn", actor);
            return current(id);
        });
    }

    /** Khóa Inventory theo ID tăng dần; mọi dòng chỉ được giữ trong cùng một transaction. */
    @Override
    public InventoryReservationResponse reserve(CreateInventoryReservationRequest request, String createdBy) {
        String actor = actor(createdBy);
        String reservationId = request.reservationId().trim();
        String operationId = request.operationId().trim();
        String orderId = request.orderId().trim();
        String payloadHash = reserveHash(request);
        return deadlockRetryExecutor.execute("reserve inventory reservation " + reservationId,
                () -> reserveInTransaction(request, actor, reservationId, operationId, orderId, payloadHash));
    }

    private InventoryReservationResponse reserveInTransaction(CreateInventoryReservationRequest request,
                                                               String actor, String reservationId,
                                                               String operationId, String orderId,
                                                               String payloadHash) {
        ReservationHeaderRecord existing = reservationRepository.findByOperationId(operationId).orElse(null);
        if (existing != null) return replayOrReject(existing, payloadHash);
        ReservationHeaderRecord sameId = reservationRepository.findById(reservationId).orElse(null);
        if (sameId != null) throw new BusinessException(ErrorCode.CONFLICT,
                "reservationId đã tồn tại với một lệnh khác");

        List<CreateInventoryReservationRequest.Line> inputLines = validateReserveLines(request.lines());
        List<String> inventoryIds = inputLines.stream().map(line -> line.inventoryId().trim()).sorted().toList();
        List<Inventory> locked = inventoryRepository.findByIdsForUpdate(inventoryIds);
        if (locked.size() != inventoryIds.size()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Một hoặc nhiều Inventory không tồn tại");
        }
        Map<String, Inventory> beforeById = new HashMap<>();
        for (Inventory inventory : locked) {
            locationAccessService.requireWritableInventory(inventory);
            beforeById.put(inventory.getId(), inventory);
        }
        String enterpriseId = locked.getFirst().getEnterpriseId();
        if (locked.stream().anyMatch(row -> !enterpriseId.equals(row.getEnterpriseId()))) {
            throw new BusinessException(ErrorCode.RESOURCE_OWNERSHIP_MISMATCH,
                    "Các Inventory của một reservation phải thuộc cùng enterprise");
        }
        if (!request.expiresAt().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.INVALID_TIME_RANGE, "expiresAt phải nằm trong tương lai");
        }

        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        ReservationHeaderRecord header = new ReservationHeaderRecord(reservationId, orderId, enterpriseId,
                operationId, payloadHash, "HELD", request.expiresAt(), 0, actor, createdAt, null, null, null);
        if (!reservationRepository.insertHeader(header)) {
            ReservationHeaderRecord winner = reservationRepository.findByOperationId(operationId).orElse(null);
            if (winner != null) return replayOrReject(winner, payloadHash);
            throw new BusinessException(ErrorCode.CONFLICT, "reservationId hoặc operationId đã được sử dụng");
        }

        List<ReservationLineRecord> savedLines = new ArrayList<>();
        List<StockMovement> movements = new ArrayList<>();
        int lineNumber = 1;
        for (CreateInventoryReservationRequest.Line line : inputLines) {
            Inventory before = beforeById.get(line.inventoryId().trim());
            Inventory after = inventoryRepository.reserve(before.getId(), line.quantity())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                            "Tồn khả dụng không đủ cho SKU " + before.getSku()));
            String lineId = UUID.randomUUID().toString();
            savedLines.add(new ReservationLineRecord(lineId, reservationId, lineNumber++, before.getId(),
                    before.getSku(), line.quantity(), BigDecimal.ZERO, BigDecimal.ZERO));
            movements.add(movement(before, after, MovementType.RESERVE, line.quantity(),
                    "ORDER_RESERVATION", reservationId, "Giữ hàng cho đơn " + orderId, actor));
            beforeById.put(before.getId(), after);
        }
        reservationRepository.insertLines(reservationId, savedLines);
        movementRepository.insertMovements(movements);
        return reservationRepository.toResponse(header, savedLines);
    }

    /** Chuyển HELD sang COMMITTED một lần; đồng hồ PostgreSQL là căn cứ quyết định hết hạn. */
    @Override
    public InventoryReservationResponse commit(String reservationId, CommitInventoryReservationRequest request,
                                               String committedBy) {
        String actor = actor(committedBy);
        String id = reservationId.trim();
        String hash = hash(String.join("\n", id, request.operationId().trim(), request.orderId().trim(),
                request.verifiedPaymentReference().trim(), request.expectedVersion().toString()));
        return deadlockRetryExecutor.execute("commit reservation " + id,
                () -> commitInTransaction(id, request, actor, hash));
    }

    private InventoryReservationResponse commitInTransaction(String id, CommitInventoryReservationRequest request,
                                                              String actor, String hash) {
        ReservationHeaderRecord header = lockHeader(id);
        if (replayOperation(header, request.operationId(), "COMMIT", hash)) return current(header.id());
        checkVersion(header, request.expectedVersion());
        if (!header.orderId().equals(request.orderId().trim())) {
            throw new BusinessException(ErrorCode.RESOURCE_OWNERSHIP_MISMATCH,
                    "orderId không khớp reservation");
        }
        if (!"HELD".equals(header.status())) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Chỉ reservation HELD mới được commit");
        }
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        if (!reservationRepository.commit(id, request.expectedVersion(), request.orderId().trim(),
                request.operationId().trim(), request.verifiedPaymentReference().trim(), actor, now)) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Reservation đã hết hạn hoặc vừa được cập nhật; không thể commit");
        }
        ReservationHeaderRecord after = lockHeader(id);
        List<StockMovement> movements = new ArrayList<>();
        for (ReservationLineRecord line : reservationRepository.findLines(id)) {
            Inventory inventory = inventoryRepository.findInventoryRowByIdForUpdate(line.inventoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy Inventory dòng reservation"));
            movements.add(movement(inventory, inventory, MovementType.COMMIT, line.requestedQuantity(),
                    "ORDER_RESERVATION", id, "Commit sau xác nhận thanh toán "
                            + request.verifiedPaymentReference().trim(), actor));
        }
        movementRepository.insertMovements(movements);
        saveOperation(id, request.operationId(), "COMMIT", hash, after,
                request.verifiedPaymentReference().trim());
        return current(id);
    }

    /** Release theo từng line; việc giảm bucket và tăng releasedQuantity commit cùng nhau. */
    @Override
    public InventoryReservationResponse release(String reservationId, ReleaseInventoryReservationRequest request,
                                                String releasedBy) {
        String actor = actor(releasedBy);
        String id = reservationId.trim();
        String hash = releaseHash(id, request);
        return deadlockRetryExecutor.execute("release reservation " + id,
                () -> releaseInTransaction(id, request, actor, hash));
    }

    private InventoryReservationResponse releaseInTransaction(String id, ReleaseInventoryReservationRequest request,
                                                               String actor, String hash) {
        ReservationHeaderRecord header = lockHeader(id);
        if (replayOperation(header, request.operationId(), "RELEASE", hash)) return current(id);
        checkVersion(header, request.expectedVersion());
        if (!List.of("HELD", "COMMITTED", "PARTIALLY_DISPATCHED").contains(header.status())) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Reservation không còn lượng có thể release: " + header.status());
        }
        List<ReservationLineRecord> allLines = reservationRepository.findLines(id);
        List<LineQuantity> releaseLines = resolveReleaseLines(allLines, request.lines());
        applyRelease(id, releaseLines, request.reason().trim(), actor);
        String statusAfter = statusAfterLines(reservationRepository.findLines(id), header.status());
        if (!reservationRepository.updateStatus(id, header.version(), header.status(), statusAfter)) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT, "Reservation vừa thay đổi; tải lại rồi thử lại");
        }
        ReservationHeaderRecord after = lockHeader(id);
        saveOperation(id, request.operationId(), "RELEASE", hash, after);
        return current(id);
    }

    /** Dispatch trừ đúng phần đã giữ; không cho shipment khác dùng lại cùng lượng reservation line. */
    @Override
    public InventoryReservationResponse dispatch(String reservationId, DispatchInventoryReservationRequest request,
                                                 String dispatchedBy) {
        String actor = actor(dispatchedBy);
        String id = reservationId.trim();
        String hash = dispatchHash(id, request);
        return deadlockRetryExecutor.execute("dispatch reservation " + id,
                () -> dispatchInTransaction(id, request, actor, hash));
    }

    private InventoryReservationResponse dispatchInTransaction(String id,
                                                                DispatchInventoryReservationRequest request,
                                                                String actor, String hash) {
        ReservationHeaderRecord header = lockHeader(id);
        if (replayOperation(header, request.operationId(), "DISPATCH", hash)) return current(id);
        checkVersion(header, request.expectedVersion());
        if (!header.orderId().equals(request.orderId().trim())) {
            throw new BusinessException(ErrorCode.RESOURCE_OWNERSHIP_MISMATCH,
                    "orderId không khớp reservation");
        }
        if (!List.of("COMMITTED", "PARTIALLY_DISPATCHED").contains(header.status())) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Chỉ reservation đã commit mới được dispatch");
        }
        List<ReservationLineRecord> allLines = reservationRepository.findLines(id);
        List<LineQuantity> quantities = validateDispatchLines(allLines, request.lines());
        List<String> inventoryIds = quantities.stream().map(q -> q.line().inventoryId()).distinct().sorted().toList();
        List<Inventory> lockedInventory = inventoryRepository.findByIdsForUpdate(inventoryIds);
        Map<String, Inventory> inventoryById = new HashMap<>();
        lockedInventory.forEach(row -> inventoryById.put(row.getId(), row));
        if (lockedInventory.size() != inventoryIds.size()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy một Inventory cần dispatch");
        }

        List<StockMovement> movements = new ArrayList<>();
        for (LineQuantity item : quantities) {
            ReservationLineRecord line = item.line();
            Inventory before = inventoryById.get(line.inventoryId());
            Inventory after = inventoryRepository.dispatchReserved(before.getId(), item.quantity())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                            "Tồn đã giữ không còn đủ để dispatch SKU " + line.sku()));
            if (!reservationRepository.addDispatched(line.id(), id, item.quantity())) {
                throw new BusinessException(ErrorCode.INVALID_QUANTITY,
                        "Dispatch vượt lượng còn lại của reservation line " + line.id());
            }
            movements.add(movement(before, after, MovementType.ISSUE, item.quantity(),
                    "ORDER_SHIPMENT", request.shipmentId().trim(),
                    "Dispatch theo reservation " + id, actor));
            inventoryById.put(before.getId(), after);
        }
        movementRepository.insertMovements(movements);
        List<ReservationLineRecord> afterLines = reservationRepository.findLines(id);
        String statusAfter = statusAfterLines(afterLines, header.status());
        if (!reservationRepository.updateStatus(id, header.version(), header.status(), statusAfter)) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT, "Reservation vừa thay đổi; dispatch đã rollback");
        }
        ReservationHeaderRecord after = lockHeader(id);
        saveOperation(id, request.operationId(), "DISPATCH", hash, after, request.shipmentId().trim());
        return current(id);
    }

    private void applyRelease(String reservationId, List<LineQuantity> quantities, String reason, String actor) {
        List<String> inventoryIds = quantities.stream().map(q -> q.line().inventoryId()).distinct().sorted().toList();
        List<Inventory> locked = inventoryRepository.findByIdsForUpdate(inventoryIds);
        Map<String, Inventory> byId = new HashMap<>();
        locked.forEach(row -> byId.put(row.getId(), row));
        if (locked.size() != inventoryIds.size()) throw new BusinessException(ErrorCode.NOT_FOUND,
                "Không tìm thấy một Inventory cần release");
        List<StockMovement> movements = new ArrayList<>();
        for (LineQuantity item : quantities) {
            Inventory before = byId.get(item.line().inventoryId());
            Inventory after = inventoryRepository.release(before.getId(), item.quantity())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_QUANTITY,
                            "Reserved quantity không đủ để release"));
            if (!reservationRepository.addReleased(item.line().id(), reservationId, item.quantity())) {
                throw new BusinessException(ErrorCode.INVALID_QUANTITY,
                        "Release vượt lượng chưa dispatch của reservation line");
            }
            movements.add(movement(before, after, MovementType.RELEASE, item.quantity(),
                    "ORDER_RESERVATION", reservationId, reason, actor));
            byId.put(before.getId(), after);
        }
        movementRepository.insertMovements(movements);
    }

    private List<LineQuantity> resolveReleaseLines(List<ReservationLineRecord> lines,
            List<ReleaseInventoryReservationRequest.Line> requested) {
        Map<String, ReservationLineRecord> byId = new HashMap<>();
        lines.forEach(line -> byId.put(line.id(), line));
        List<LineQuantity> result = new ArrayList<>();
        if (requested == null || requested.isEmpty()) {
            for (ReservationLineRecord line : lines) {
                BigDecimal remaining = remaining(line);
                if (remaining.signum() > 0) result.add(new LineQuantity(line, remaining));
            }
            return result;
        }
        Set<String> seen = new HashSet<>();
        for (ReleaseInventoryReservationRequest.Line input : requested) {
            String lineId = input.reservationLineId().trim();
            if (!seen.add(lineId)) throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "reservationLineId bị lặp trong request");
            ReservationLineRecord line = byId.get(lineId);
            if (line == null) throw new BusinessException(ErrorCode.NOT_FOUND,
                    "reservationLineId không thuộc reservation này: " + lineId);
            if (input.quantity().compareTo(remaining(line)) > 0) throw new BusinessException(ErrorCode.INVALID_QUANTITY,
                    "quantity vượt lượng chưa dispatch/chưa release của line " + lineId);
            result.add(new LineQuantity(line, input.quantity()));
        }
        return result;
    }

    private List<LineQuantity> validateDispatchLines(List<ReservationLineRecord> lines,
            List<DispatchInventoryReservationRequest.Line> requested) {
        Map<String, ReservationLineRecord> byId = new HashMap<>();
        lines.forEach(line -> byId.put(line.id(), line));
        Set<String> seen = new HashSet<>();
        List<LineQuantity> result = new ArrayList<>();
        for (DispatchInventoryReservationRequest.Line input : requested) {
            String lineId = input.reservationLineId().trim();
            if (!seen.add(lineId)) throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "reservationLineId bị lặp trong request");
            ReservationLineRecord line = byId.get(lineId);
            if (line == null) throw new BusinessException(ErrorCode.NOT_FOUND,
                    "reservationLineId không thuộc reservation này: " + lineId);
            if (input.quantity().compareTo(remaining(line)) > 0) throw new BusinessException(ErrorCode.INVALID_QUANTITY,
                    "Dispatch vượt lượng còn lại của line " + lineId);
            result.add(new LineQuantity(line, input.quantity()));
        }
        return result;
    }

    private List<CreateInventoryReservationRequest.Line> validateReserveLines(
            List<CreateInventoryReservationRequest.Line> lines) {
        Set<String> seen = new HashSet<>();
        List<CreateInventoryReservationRequest.Line> normalized = new ArrayList<>();
        for (CreateInventoryReservationRequest.Line line : lines) {
            String inventoryId = line.inventoryId().trim();
            if (!seen.add(inventoryId)) throw new BusinessException(ErrorCode.INVALID_OPERATION,
                    "Mỗi Inventory chỉ được xuất hiện một lần trong reservation request");
            normalized.add(new CreateInventoryReservationRequest.Line(inventoryId, line.quantity()));
        }
        return normalized;
    }

    private String statusAfterLines(List<ReservationLineRecord> lines, String previousStatus) {
        boolean remaining = lines.stream().anyMatch(line -> remaining(line).signum() > 0);
        if (remaining) return lines.stream().anyMatch(line -> line.dispatchedQuantity().signum() > 0)
                ? "PARTIALLY_DISPATCHED" : previousStatus;
        BigDecimal dispatched = lines.stream().map(ReservationLineRecord::dispatchedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal released = lines.stream().map(ReservationLineRecord::releasedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (dispatched.signum() == 0) return "RELEASED";
        if (released.signum() > 0) return "CONSUMED_PARTIAL";
        return "CONSUMED";
    }

    private BigDecimal remaining(ReservationLineRecord line) {
        return line.requestedQuantity().subtract(line.dispatchedQuantity()).subtract(line.releasedQuantity());
    }

    private boolean replayOperation(ReservationHeaderRecord header, String operationId,
                                    String type, String expectedHash) {
        ReservationOperationRecord existing = reservationRepository.findOperation(header.id(), operationId.trim())
                .orElse(null);
        if (existing == null) return false;
        if (!existing.operationType().equals(type)
                || !MessageDigest.isEqual(existing.payloadHash().getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII))) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "operationId đã được sử dụng cho thao tác hoặc nội dung khác");
        }
        return true;
    }

    private void saveOperation(String reservationId, String operationId, String type, String payloadHash,
                               ReservationHeaderRecord after) {
        saveOperation(reservationId, operationId, type, payloadHash, after, null);
    }

    private void saveOperation(String reservationId, String operationId, String type, String payloadHash,
                               ReservationHeaderRecord after, String referenceId) {
        if (!reservationRepository.insertOperation(new ReservationOperationRecord(UUID.randomUUID().toString(),
                reservationId, operationId.trim(), type, payloadHash, after.status(), after.version(),
                referenceId, Instant.now()))) {
            throw new BusinessException(ErrorCode.CONFLICT, "operationId vừa được request khác sử dụng");
        }
    }

    private ReservationHeaderRecord lockHeader(String id) {
        return reservationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy reservation: " + id));
    }

    private InventoryReservationResponse current(String id) {
        ReservationHeaderRecord header = lockHeader(id);
        return reservationRepository.toResponse(header, reservationRepository.findLines(id));
    }

    private InventoryReservationResponse replayOrReject(ReservationHeaderRecord existing, String hash) {
        if (!MessageDigest.isEqual(existing.payloadHash().getBytes(StandardCharsets.US_ASCII),
                hash.getBytes(StandardCharsets.US_ASCII))) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "operationId đã được dùng để tạo reservation với payload khác");
        }
        return current(existing.id());
    }

    private void checkVersion(ReservationHeaderRecord header, long expectedVersion) {
        if (header.version() != expectedVersion) throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                "Reservation đã thay đổi; hãy tải trạng thái mới nhất");
    }

    private StockMovement movement(Inventory before, Inventory after, MovementType type, BigDecimal quantity,
                                   String referenceType, String referenceId, String reason, String actor) {
        return StockMovement.builder().id(UUID.randomUUID().toString()).inventoryId(before.getId())
                .movementType(type).quantity(quantity).physicalBefore(before.getPhysicalQuantity())
                .physicalAfter(after.getPhysicalQuantity()).reservedBefore(before.getReservedQuantity())
                .reservedAfter(after.getReservedQuantity()).referenceType(referenceType)
                .referenceId(referenceId).reason(reason).performedBy(actor).performedBySubject(actor).build();
    }

    private String reserveHash(CreateInventoryReservationRequest request) {
        List<String> parts = new ArrayList<>(List.of(request.reservationId().trim(), request.orderId().trim(),
                request.expiresAt().toString()));
        for (CreateInventoryReservationRequest.Line line : request.lines()) {
            parts.add(line.inventoryId().trim()); parts.add(line.quantity().stripTrailingZeros().toPlainString());
        }
        return hash(String.join("\n", parts));
    }

    private String releaseHash(String id, ReleaseInventoryReservationRequest request) {
        List<String> parts = new ArrayList<>(List.of(id, request.operationId().trim(), request.reason().trim(),
                request.expectedVersion().toString()));
        if (request.lines() != null) request.lines().stream()
                .sorted(Comparator.comparing(ReleaseInventoryReservationRequest.Line::reservationLineId))
                .forEach(line -> { parts.add(line.reservationLineId().trim());
                    parts.add(line.quantity().stripTrailingZeros().toPlainString()); });
        return hash(String.join("\n", parts));
    }

    private String dispatchHash(String id, DispatchInventoryReservationRequest request) {
        List<String> parts = new ArrayList<>(List.of(id, request.operationId().trim(), request.shipmentId().trim(),
                request.orderId().trim(), request.expectedVersion().toString()));
        request.lines().stream().sorted(Comparator.comparing(DispatchInventoryReservationRequest.Line::reservationLineId))
                .forEach(line -> { parts.add(line.reservationLineId().trim());
                    parts.add(line.quantity().stripTrailingZeros().toPlainString()); });
        return hash(String.join("\n", parts));
    }

    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 không có sẵn", exception); }
    }

    private String actor(String value) { return value == null || value.isBlank() ? "unknown" : value.trim(); }
    private record LineQuantity(ReservationLineRecord line, BigDecimal quantity) {}
}
