package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.OpenInventoryCountSessionRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.RecordInventoryCountRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.RequestInventoryCountRecountRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSessionResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSessionLineResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSubmissionResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSessionSubmitResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSessionApprovalResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountRecountResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.Inventory;
import com.dtthuan3.ecommerce.inventoryservice.entity.MovementType;
import com.dtthuan3.ecommerce.inventoryservice.entity.StockMovement;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.StockMovementRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryCountSessionRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryCountSessionRepository.CountObservationRecord;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryCountSessionRepository.CountSubmissionRecord;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryCountSessionRepository.InventoryCountSessionRecord;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryCountSessionRepository.SessionState;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryCountSessionRepository.CountSummary;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryCountSessionRepository.CountApprovalLine;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryCountSessionRepository.CountReviewRecord;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryCountSessionService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryLocationAccessService;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryLocationAccessService.Position;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Mở phiên kiểm kê, khóa phạm vi tồn trong thời gian snapshot ngắn và lưu baseline bất biến. */
@Service
@RequiredArgsConstructor
public class InventoryCountSessionServiceImpl implements InventoryCountSessionService {
    private final InventoryCountSessionRepository countSessionRepository;
    private final InventoryLocationAccessService locationAccessService;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final DeadlockRetryExecutor deadlockRetryExecutor;

    /** Liệt kê session theo enterprise/kho/trạng thái; database đảm nhiệm lọc và phân trang. */
    @Override
    @Transactional(readOnly = true)
    public Page<InventoryCountSessionResponse> list(String enterpriseId, String warehouseId,
                                                     String status, Pageable pageable) {
        if (enterpriseId == null || enterpriseId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION, "enterpriseId là bắt buộc");
        }
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim().toUpperCase();
        return countSessionRepository.searchSessions(enterpriseId.trim(),
                warehouseId == null || warehouseId.isBlank() ? null : warehouseId.trim(),
                normalizedStatus, pageable);
    }

    /** Lấy trang baseline theo sessionId; kiểm tra header trước để phân biệt session không tồn tại. */
    @Override
    @Transactional(readOnly = true)
    public Page<InventoryCountSessionLineResponse> getLines(String sessionId, int page, int size) {
        if (!countSessionRepository.existsSession(sessionId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy phiên kiểm kê: " + sessionId);
        }
        return countSessionRepository.findLines(sessionId,
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200))));
    }

    /** Ghi một lô observation bất biến; version phiên tuần tự hóa các lô và chống ghi trên snapshot cũ. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryCountSubmissionResponse recordCounts(String sessionId,
                                                         RecordInventoryCountRequest request,
                                                         String recordedBy) {
        String normalizedSessionId = sessionId.trim();
        String operationId = request.operationId().trim();
        String actor = recordedBy == null || recordedBy.isBlank() ? "unknown" : recordedBy.trim();
        List<NormalizedCountLine> lines = normalizeLines(request.lines());
        String payloadHash = countPayloadHash(request.expectedVersion(), lines);

        CountSubmissionRecord previous = countSessionRepository.findSubmission(normalizedSessionId, operationId)
                .orElse(null);
        if (previous != null) return replayOrReject(previous, payloadHash);

        SessionState session = countSessionRepository.lockSession(normalizedSessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy phiên kiểm kê: " + normalizedSessionId));

        // Request có thể đã được một worker ghi trong lúc request này chờ khóa header phiên.
        previous = countSessionRepository.findSubmission(normalizedSessionId, operationId).orElse(null);
        if (previous != null) return replayOrReject(previous, payloadHash);

        if (!"OPEN".equals(session.status())) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Chỉ phiên kiểm kê OPEN mới nhận được số đếm");
        }
        if (session.version() != request.expectedVersion()) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiên kiểm kê đã thay đổi; hãy tải lại version mới nhất");
        }

        List<String> inventoryIds = lines.stream().map(NormalizedCountLine::inventoryId).toList();
        if (countSessionRepository.countLinesBelongingToSession(normalizedSessionId, inventoryIds)
                != inventoryIds.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Một hoặc nhiều inventoryId không thuộc snapshot của phiên kiểm kê này");
        }

        LocalDateTime now = LocalDateTime.now();
        String submissionId = UUID.randomUUID().toString();
        CountSubmissionRecord submission = new CountSubmissionRecord(submissionId, normalizedSessionId,
                operationId, payloadHash, lines.size(), request.expectedVersion() + 1, actor, now);
        if (!countSessionRepository.insertSubmission(submission)) {
            CountSubmissionRecord concurrent = countSessionRepository.findSubmission(normalizedSessionId, operationId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT,
                            "Lô kiểm đếm đang được ghi đồng thời; hãy gửi lại cùng operationId"));
            return replayOrReject(concurrent, payloadHash);
        }

        List<CountObservationRecord> observations = new ArrayList<>(lines.size());
        for (NormalizedCountLine line : lines) {
            observations.add(new CountObservationRecord(UUID.randomUUID().toString(), submissionId,
                    normalizedSessionId, line.inventoryId(), line.goodQuantity(), line.defectiveQuantity(),
                    line.pendingQuantity(), line.reason(), line.evidenceReference(), actor, now));
        }
        countSessionRepository.insertObservations(observations);

        if (!countSessionRepository.advanceVersion(normalizedSessionId, request.expectedVersion())) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiên kiểm kê đã đổi version; lô số đếm đã được hoàn tác");
        }
        return toSubmissionResponse(submission);
    }

    /** Chỉ gửi duyệt khi mọi dòng baseline đã được đếm; API này không tự ý sửa số dư kho. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryCountSessionSubmitResponse submit(String sessionId) {
        String normalizedSessionId = sessionId.trim();
        SessionState session = countSessionRepository.lockSession(normalizedSessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy phiên kiểm kê: " + normalizedSessionId));

        // Retry submit sau khi đã thành công trả lại cùng báo cáo, không tăng version lần nữa.
        if ("PENDING_APPROVAL".equals(session.status())) {
            return toSubmitResponse(normalizedSessionId, session.version(),
                    countSessionRepository.summarizeLatestCounts(normalizedSessionId));
        }
        if (!"OPEN".equals(session.status())) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Chỉ phiên OPEN mới có thể gửi duyệt");
        }
        long uncounted = countSessionRepository.countUncountedLines(normalizedSessionId);
        if (uncounted > 0) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Còn " + uncounted + " dòng baseline chưa được kiểm đếm");
        }

        CountSummary summary = countSessionRepository.summarizeLatestCounts(normalizedSessionId);
        if (!countSessionRepository.submitSession(normalizedSessionId, session.version())) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiên kiểm kê vừa thay đổi; hãy tải lại trước khi gửi duyệt");
        }
        return toSubmitResponse(normalizedSessionId, session.version() + 1, summary);
    }

    private InventoryCountSessionSubmitResponse toSubmitResponse(String sessionId, long version,
                                                                  CountSummary summary) {
        return new InventoryCountSessionSubmitResponse(sessionId, "PENDING_APPROVAL", version,
                summary.lineCount(), summary.goodVariance(), summary.defectiveVariance(),
                summary.pendingVariance());
    }

    /** Duyệt chạy retry ngoài transaction để mỗi lần retry bắt đầu bằng transaction PostgreSQL mới. */
    @Override
    public InventoryCountSessionApprovalResponse approve(String sessionId, String approvedBy) {
        String actor = approvedBy == null || approvedBy.isBlank() ? "unknown" : approvedBy.trim();
        return deadlockRetryExecutor.execute("duyệt phiên kiểm kê",
                () -> approveInTransaction(sessionId.trim(), actor));
    }

    /** Cập nhật bucket, ghi StockMovement/outbox và chốt phiên trong cùng transaction. */
    @Transactional(rollbackFor = Exception.class)
    protected InventoryCountSessionApprovalResponse approveInTransaction(String sessionId, String actor) {
        SessionState session = countSessionRepository.lockSession(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy phiên kiểm kê: " + sessionId));

        // Retry sau commit chỉ trả lại báo cáo; tuyệt đối không phát lại các điều chỉnh tồn.
        if ("APPROVED".equals(session.status())) {
            CountSummary summary = countSessionRepository.summarizeLatestCounts(sessionId);
            return toApprovalResponse(sessionId, session.version(), summary, 0);
        }
        if (!"PENDING_APPROVAL".equals(session.status())) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Chỉ phiên PENDING_APPROVAL mới được duyệt");
        }

        List<CountApprovalLine> lines = countSessionRepository.lockApprovalLines(sessionId);
        CountSummary summary = countSessionRepository.summarizeLatestCounts(sessionId);
        List<StockMovement> movements = new ArrayList<>();
        long adjustedLines = 0;
        LocalDateTime now = LocalDateTime.now();

        for (CountApprovalLine line : lines) {
            // Không áp dụng số đếm trên balance đã phát sinh movement kể từ snapshot.
            if (line.currentVersion() != line.snapshotVersion()) {
                throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                        "Inventory " + line.inventoryId()
                                + " đã thay đổi sau snapshot; cần đối soát movement và kiểm đếm lại trước khi duyệt");
            }
            if (line.countedGood().compareTo(line.currentReserved()) < 0) {
                throw new BusinessException(ErrorCode.INVALID_QUANTITY,
                        "Số lượng hàng tốt kiểm đếm thấp hơn lượng đã giữ tại Inventory " + line.inventoryId());
            }

            boolean changed = line.countedGood().compareTo(
                    line.currentPhysical().subtract(line.currentDefective()).subtract(line.currentPending())) != 0
                    || line.countedDefective().compareTo(line.currentDefective()) != 0
                    || line.countedPending().compareTo(line.currentPending()) != 0;
            if (!changed) continue;

            Inventory updated = inventoryRepository.adjustCountedBuckets(line.inventoryId(), line.countedGood(),
                            line.countedDefective(), line.countedPending(), line.snapshotVersion())
                    .orElseThrow(() -> new BusinessException(ErrorCode.VERSION_CONFLICT,
                            "Inventory thay đổi đồng thời hoặc số đếm không hợp lệ: " + line.inventoryId()));
            BigDecimal physicalAfter = updated.getPhysicalQuantity();
            BigDecimal movementQuantity = physicalAfter.subtract(line.currentPhysical()).abs();
            String reason = "Duyệt kiểm kê " + sessionId + ": " + line.countReason();
            movements.add(StockMovement.builder()
                    .id(UUID.randomUUID().toString())
                    .inventoryId(line.inventoryId())
                    .movementType(MovementType.ADJUSTMENT)
                    .quantity(movementQuantity)
                    .physicalBefore(line.currentPhysical())
                    .physicalAfter(physicalAfter)
                    .reservedBefore(line.currentReserved())
                    .reservedAfter(line.currentReserved())
                    .referenceType("INVENTORY_COUNT")
                    .referenceId(sessionId)
                    .reason(reason.length() > 500 ? reason.substring(0, 500) : reason)
                    .performedBy(actor)
                    .performedBySubject(actor)
                    .createdAt(now)
                    .build());
            adjustedLines++;
        }

        // Insert movement cùng outbox trong transaction; lỗi phát sinh sẽ rollback cả bucket và session.
        stockMovementRepository.insertMovements(movements);
        if (!countSessionRepository.approveSession(sessionId, session.version(), actor, now)) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiên kiểm kê vừa thay đổi; các cập nhật tồn đã được hoàn tác");
        }
        return toApprovalResponse(sessionId, session.version() + 1, summary, adjustedLines);
    }

    private InventoryCountSessionApprovalResponse toApprovalResponse(String sessionId, long version,
                                                                      CountSummary summary, long adjustedLines) {
        return new InventoryCountSessionApprovalResponse(sessionId, "APPROVED", version,
                summary.lineCount(), adjustedLines, summary.goodVariance(), summary.defectiveVariance(),
                summary.pendingVariance());
    }

    /** Yêu cầu recount chuyển phiên về OPEN và append quyết định audit với idempotency theo operationId. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryCountRecountResponse requestRecount(String sessionId,
                                                        RequestInventoryCountRecountRequest request,
                                                        String requestedBy) {
        String normalizedSessionId = sessionId.trim();
        String operationId = request.operationId().trim();
        String actor = requestedBy == null || requestedBy.isBlank() ? "unknown" : requestedBy.trim();
        String reason = request.reason().trim();
        String hash = recountPayloadHash(request.expectedVersion(), reason);

        CountReviewRecord previous = countSessionRepository.findCountReview(normalizedSessionId, operationId)
                .orElse(null);
        if (previous != null) return replayRecountOrReject(previous, hash);

        SessionState session = countSessionRepository.lockSession(normalizedSessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy phiên kiểm kê: " + normalizedSessionId));
        // Kiểm tra lần nữa sau khi lấy khóa để hai request đồng thời cùng operationId chỉ tạo một audit record.
        previous = countSessionRepository.findCountReview(normalizedSessionId, operationId).orElse(null);
        if (previous != null) return replayRecountOrReject(previous, hash);
        if (!"PENDING_APPROVAL".equals(session.status())) {
            throw new BusinessException(ErrorCode.INVALID_RESOURCE_STATE,
                    "Chỉ phiên PENDING_APPROVAL mới được yêu cầu đếm lại");
        }
        if (session.version() != request.expectedVersion()) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiên kiểm kê đã thay đổi; hãy tải lại version mới nhất");
        }

        LocalDateTime now = LocalDateTime.now();
        CountReviewRecord review = new CountReviewRecord(UUID.randomUUID().toString(), normalizedSessionId,
                operationId, hash, reason, actor, session.version() + 1, now);
        if (!countSessionRepository.insertCountReview(review)) {
            CountReviewRecord concurrent = countSessionRepository.findCountReview(normalizedSessionId, operationId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT,
                            "Yêu cầu đếm lại đang được xử lý; hãy gửi lại cùng operationId"));
            return replayRecountOrReject(concurrent, hash);
        }
        if (!countSessionRepository.reopenForRecount(normalizedSessionId, session.version())) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                    "Phiên đã đổi trạng thái; yêu cầu đếm lại đã được hoàn tác");
        }
        return review.toResponse();
    }

    private InventoryCountRecountResponse replayRecountOrReject(CountReviewRecord existing, String payloadHash) {
        if (!MessageDigest.isEqual(existing.payloadHash().getBytes(StandardCharsets.US_ASCII),
                payloadHash.getBytes(StandardCharsets.US_ASCII))) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "operationId đã được dùng cho yêu cầu đếm lại với version hoặc lý do khác");
        }
        return existing.toResponse();
    }

    private String recountPayloadHash(long expectedVersion, String reason) {
        String canonical = expectedVersion + "\n" + encodeCanonical(reason);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không có sẵn trong JVM", exception);
        }
    }

    /** Idempotency được kiểm tra trước snapshot để retry không khóa tồn hoặc chụp lại baseline. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryCountSessionResponse open(OpenInventoryCountSessionRequest request, String openedBy) {
        String enterpriseId = request.enterpriseId().trim();
        String warehouseId = request.warehouseId().trim();
        String locationId = normalizeOptional(request.locationId());
        String sku = normalizeOptional(request.sku());
        String operationId = request.operationId().trim();
        String reason = request.reason().trim();
        String actor = openedBy == null || openedBy.isBlank() ? "unknown" : openedBy.trim();
        String payloadHash = payloadHash(enterpriseId, warehouseId, locationId, sku, reason);

        InventoryCountSessionResponse existing = countSessionRepository.findByOperation(enterpriseId, operationId)
                .orElse(null);
        if (existing != null) return replayOrReject(existing, payloadHash, enterpriseId, operationId);

        // Xác nhận kho đang hoạt động và đúng doanh nghiệp; nếu có scope Location thì xác thực cả Location.
        if (locationId == null) {
            locationAccessService.requireActiveWarehouse(warehouseId, enterpriseId);
        } else {
            locationAccessService.requireWritablePosition(enterpriseId, warehouseId, locationId);
        }

        LocalDateTime snapshotAt = LocalDateTime.now();
        InventoryCountSessionRecord candidate = new InventoryCountSessionRecord(
                UUID.randomUUID().toString(), enterpriseId, warehouseId, locationId, sku, operationId,
                payloadHash, "OPEN", reason, actor, snapshotAt, snapshotAt, 0L);

        if (countSessionRepository.insertSession(candidate)) {
            long lineCount = countSessionRepository.snapshotInventory(candidate);
            if (lineCount == 0) {
                throw new BusinessException(ErrorCode.NOT_FOUND,
                        "Phạm vi kiểm kê chưa có Inventory nào để chụp baseline");
            }
            return new InventoryCountSessionResponse(candidate.id(), enterpriseId, warehouseId, locationId, sku,
                    "OPEN", reason, actor, snapshotAt, lineCount, 0L);
        }

        // INSERT ... ON CONFLICT xử lý hai lần mở đồng thời dùng cùng idempotency key.
        InventoryCountSessionResponse winner = countSessionRepository.findByOperation(enterpriseId, operationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT,
                        "Phiên kiểm kê đang được tạo đồng thời; hãy gửi lại cùng operationId"));
        return replayOrReject(winner, payloadHash, enterpriseId, operationId);
    }

    private InventoryCountSessionResponse replayOrReject(InventoryCountSessionResponse existing, String payloadHash,
                                                           String enterpriseId, String operationId) {
        String storedHash = countSessionRepository.findPayloadHash(enterpriseId, operationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "Không đọc được dấu idempotency của phiên kiểm kê"));
        if (!MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.US_ASCII),
                payloadHash.getBytes(StandardCharsets.US_ASCII))) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "operationId đã được dùng để mở phiên kiểm kê với phạm vi hoặc lý do khác");
        }
        return existing;
    }

    private InventoryCountSubmissionResponse replayOrReject(CountSubmissionRecord existing, String payloadHash) {
        if (!MessageDigest.isEqual(existing.payloadHash().getBytes(StandardCharsets.US_ASCII),
                payloadHash.getBytes(StandardCharsets.US_ASCII))) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "operationId đã được dùng cho lô kiểm đếm có nội dung khác");
        }
        return toSubmissionResponse(existing);
    }

    private InventoryCountSubmissionResponse toSubmissionResponse(CountSubmissionRecord submission) {
        return new InventoryCountSubmissionResponse(submission.sessionId(), submission.operationId(),
                submission.lineCount(), submission.versionAfter(), submission.createdAt());
    }

    /** Chuẩn hóa dữ liệu trước khi hash và chặn cùng một balance xuất hiện hai lần trong một batch. */
    private List<NormalizedCountLine> normalizeLines(List<RecordInventoryCountRequest.Line> requestedLines) {
        List<NormalizedCountLine> lines = new ArrayList<>(requestedLines.size());
        Set<String> inventoryIds = new HashSet<>();
        for (RecordInventoryCountRequest.Line line : requestedLines) {
            String inventoryId = line.inventoryId().trim();
            if (!inventoryIds.add(inventoryId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "Một lô kiểm đếm không được chứa trùng inventoryId: " + inventoryId);
            }
            lines.add(new NormalizedCountLine(inventoryId, line.countedGoodQuantity(),
                    line.countedDefectiveQuantity(), line.countedPendingQuantity(), line.countReason().trim(),
                    normalizeOptional(line.evidenceReference())));
        }
        return lines;
    }

    /** Hash độc lập thứ tự các dòng trong JSON để retry cùng nội dung không bị coi là request mới. */
    private String countPayloadHash(long expectedVersion, List<NormalizedCountLine> lines) {
        StringBuilder canonical = new StringBuilder().append(expectedVersion);
        lines.stream().sorted(Comparator.comparing(NormalizedCountLine::inventoryId)).forEach(line -> canonical
                .append('\n').append(encodeCanonical(line.inventoryId()))
                .append('|').append(line.goodQuantity().stripTrailingZeros().toPlainString())
                .append('|').append(line.defectiveQuantity().stripTrailingZeros().toPlainString())
                .append('|').append(line.pendingQuantity().stripTrailingZeros().toPlainString())
                .append('|').append(encodeCanonical(line.reason()))
                .append('|').append(encodeCanonical(line.evidenceReference() == null ? "" : line.evidenceReference())));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không có sẵn trong JVM", exception);
        }
    }

    private String encodeCanonical(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private record NormalizedCountLine(String inventoryId, BigDecimal goodQuantity,
                                       BigDecimal defectiveQuantity,
                                       BigDecimal pendingQuantity, String reason,
                                       String evidenceReference) {}

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String payloadHash(String enterpriseId, String warehouseId, String locationId, String sku, String reason) {
        String payload = String.join("\n", enterpriseId, warehouseId,
                locationId == null ? "" : locationId, sku == null ? "" : sku, reason);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không có sẵn trong JVM", exception);
        }
    }
}
