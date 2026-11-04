package com.dtthuan3.ecommerce.reviewservice.service.impl;


import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.DuplicateResourceException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.reviewservice.util.*;
import com.dtthuan3.ecommerce.reviewservice.client.FileVerificationClient;
import com.dtthuan3.ecommerce.reviewservice.client.OrderLineClient;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewCreateRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewMediaRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewUpdateRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;
import com.dtthuan3.ecommerce.reviewservice.entity.*;
import com.dtthuan3.ecommerce.reviewservice.enums.EligibilityStatus;
import com.dtthuan3.ecommerce.reviewservice.enums.IdempotencyStatus;
import com.dtthuan3.ecommerce.reviewservice.enums.ReviewStatus;
import com.dtthuan3.ecommerce.reviewservice.enums.RevisionStatus;
import com.dtthuan3.ecommerce.reviewservice.mapper.ReviewAssembler;
import com.dtthuan3.ecommerce.reviewservice.mapper.ReviewMapper;
import com.dtthuan3.ecommerce.reviewservice.repository.*;
import com.dtthuan3.ecommerce.reviewservice.service.ReviewService;
import jakarta.ws.rs.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    // --- Quy tắc nghiệp vụ cố định (muốn đổi thì sửa ở đây) ---
    public static final int ELIGIBILITY_WINDOW_DAYS = 30;   // hạn đánh giá sau khi giao hàng
    public static final int EDIT_WINDOW_DAYS = 7;           // hạn được sửa sau khi gửi
    public static final int MAX_EDITS = 0;                  // 0 hoặc âm = không giới hạn số lần sửa
    public static final int IDEMPOTENCY_TTL_HOURS = 72;
    public static final boolean AUTO_PUBLISH = false;       // false = mọi review đều chờ duyệt (theo tài liệu)

    public static boolean canEditMore(int editsDone) {
        return MAX_EDITS <= 0 || editsDone < MAX_EDITS;
    }

    private static final String OP_CREATE_REVIEW = "CREATE_REVIEW";
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

    private final ReviewRepository reviewRepository;
    private final ReviewRevisionRepository revisionRepository;
    private final ReviewMediaRepository mediaRepository;
    private final EligibilityProjectionRepository eligibilityRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final ReviewRules rules;
    private final ReviewPublisher publisher;
    private final OutboxWriter outbox;
    private final ReviewAssembler assembler;
    private final FileVerificationClient fileClient;
    private final OrderLineClient orderClient;
    private final ReviewMetrics metrics;
    private final RatingAggregateService aggregateService;
    private final ReviewMapper mapper;

    // =====================================================================
    // CREATE  (REV-UC01)
    // =====================================================================

    @Override
    @Transactional
    public ReviewResponse createReview(String userId, String userName, String idempotencyKey,
                                       ReviewCreateRequest request) {
        String key = requireIdempotencyKey(idempotencyKey);
        List<ReviewMediaRequest> media = rules.validateMedia(request.getMedia());
        String content = ReviewRules.normalize(request.getContent());
        String fingerprint = fingerprint(request, content);
        LocalDateTime now = LocalDateTime.now();

        // 1) Replay: cùng key + cùng nội dung -> trả kết quả cũ (vẫn chỉ trả review của chính người gửi)
        Optional<IdempotencyRecord> existing = idempotencyRepository
                .findByActorIdAndOperationTypeAndIdempotencyKey(userId, OP_CREATE_REVIEW, key);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (record.getExpiresAt().isAfter(now)) {
                if (!record.getFingerprint().equals(fingerprint)) {
                    throw new BusinessException("IDEMPOTENCY_CONFLICT", HttpStatus.UNPROCESSABLE_ENTITY, "Idempotency-Key này đã được dùng cho một yêu cầu khác");
                }
                return replay(record);
            }
            idempotencyRepository.delete(record); // đã hết hạn -> cho phép dùng lại key
            idempotencyRepository.flush();
        }

        // 2) Chiếm key ngay (flush): yêu cầu song song cùng key chờ rồi bị unique -> 409 DUPLICATE_REQUEST
        IdempotencyRecord idem = idempotencyRepository.saveAndFlush(IdempotencyRecord.builder()
                .actorId(userId)
                .operationType(OP_CREATE_REVIEW)
                .idempotencyKey(key)
                .fingerprint(fingerprint)
                .status(IdempotencyStatus.PROCESSING)
                .expiresAt(now.plusHours(IDEMPOTENCY_TTL_HOURS))
                .build());

        // 3) Điều kiện được đánh giá (chỉ tin dữ liệu server: projection hoặc Order, không tin frontend)
        EligibilityProjection eligibility = resolveEligibility(request.getLineId(), userId, now);
        if (reviewRepository.findByLineId(request.getLineId()).isPresent()) {
            metrics.submitBlocked("already_exists");
            throw new DuplicateResourceException("Sản phẩm này đã được đánh giá");
        }
        fileClient.ensureReady(userId, media.stream().map(ReviewMediaRequest::getFileId).toList());

        // 4) Tạo review + revision 1 + media (trạng thái PENDING_MODERATION)
        Review review = reviewRepository.saveAndFlush(
                mapper.createReviewEntity(userId, userName, eligibility, request, content, media.size(), now));

        ReviewRevision revision = revisionRepository.save(
                mapper.createRevision(review.getId(), 1, request.getRating(), content, now));
        List<ReviewMedia> mediaList = mediaRepository.saveAll(
                mapper.createMediaList(review.getId(), revision.getId(), media));

        Map<String, Object> submitted = new LinkedHashMap<>();
        submitted.put("revisionId", revision.getId());
        submitted.put("revisionNo", 1);
        outbox.enqueue(OutboxWriter.EVENT_SUBMITTED, review, submitted);

        // 5) Mặc định chờ kiểm duyệt; chỉ tự duyệt khi bật review.auto-publish và nội dung sạch
        if (AUTO_PUBLISH && !rules.needsModeration(content)) {
            publisher.approve(review, revision, mediaList);
        }

        idem.setStatus(IdempotencyStatus.COMPLETED);
        idem.setResultRef(review.getId());
        idempotencyRepository.save(idem);

        log.info("User {} created review {} for line {} (status={})",
                userId, review.getId(), request.getLineId(), review.getStatus());
        return assembler.ownerView(review);
    }

    // =====================================================================
    // UPDATE  (REV-04: sửa trong 7 ngày -> revision mới, cần duyệt lại)
    // =====================================================================

    @Override
    @Transactional
    public ReviewResponse updateReview(String userId, String reviewId, ReviewUpdateRequest request) {
        List<ReviewMediaRequest> media = rules.validateMedia(request.getMedia());
        String content = ReviewRules.normalize(request.getContent());
        LocalDateTime now = LocalDateTime.now();

        // Khóa review: tuần tự hóa với kiểm duyệt / xóa / sửa song song
        Review review = reviewRepository.findForUpdate(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));
        requireOwner(review, userId);

        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new BusinessException("REVIEW_DELETED", HttpStatus.UNPROCESSABLE_ENTITY, "Đánh giá đã bị xóa");
        }
        if (review.getStatus() == ReviewStatus.HIDDEN) {
            throw new BusinessException("REVIEW_LOCKED", HttpStatus.UNPROCESSABLE_ENTITY, "Đánh giá đang bị ẩn, không thể chỉnh sửa");
        }

        ReviewRevision latest = revisionRepository
                .findByReviewIdAndRevisionNo(review.getId(), review.getLatestRevisionNo()).orElse(null);
        if (latest != null && isUnchanged(latest, request.getRating(), content, media)) {
            return assembler.ownerView(review); // PUT lặp lại cùng nội dung: không tạo revision rác
        }

        if (review.getEditableUntil() == null || review.getEditableUntil().isBefore(now)) {
            throw new BusinessException("REVIEW_EDIT_WINDOW_EXPIRED", HttpStatus.UNPROCESSABLE_ENTITY, "Đã quá " + EDIT_WINDOW_DAYS + " ngày, không thể sửa đánh giá");
        }
        if (!canEditMore(review.getLatestRevisionNo() - 1)) {
            throw new BusinessException("REVIEW_EDIT_LIMIT_REACHED", HttpStatus.UNPROCESSABLE_ENTITY, "Bạn chỉ có thể sửa đánh giá tối đa " + MAX_EDITS + " lần");
        }
        if (!Objects.equals(review.getVersion(), request.getVersion())) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT, "Đánh giá đã được cập nhật ở nơi khác, vui lòng tải lại");
        }
        fileClient.ensureReady(userId, media.stream().map(ReviewMediaRequest::getFileId).toList());

        // Bản chờ duyệt trước đó (nếu còn) bị thay thế; KHÔNG xóa revision hay quyết định cũ
        if (latest != null && latest.getStatus() == RevisionStatus.PENDING_MODERATION) {
            latest.setStatus(RevisionStatus.SUPERSEDED);
        }

        int newNo = review.getLatestRevisionNo() + 1;
        review.setLatestRevisionNo(newNo);
        if (review.getPublishedRevisionId() == null) {
            // Chưa từng được duyệt: cập nhật luôn thông tin đang chờ
            mapper.applyEditToUnpublished(review, request.getRating(), content, media.size());
        }
        // Đã APPROVED: GIỮ NGUYÊN bản đang hiển thị (và điểm tổng) cho tới khi bản sửa được quyết định
        review = reviewRepository.saveAndFlush(review);

        ReviewRevision revision = revisionRepository.save(
                mapper.createRevision(review.getId(), newNo, request.getRating(), content, now));
        List<ReviewMedia> mediaList = mediaRepository.saveAll(
                mapper.createMediaList(review.getId(), revision.getId(), media));

        Map<String, Object> submitted = new LinkedHashMap<>();
        submitted.put("revisionId", revision.getId());
        submitted.put("revisionNo", newNo);
        outbox.enqueue(OutboxWriter.EVENT_SUBMITTED, review, submitted);

        if (AUTO_PUBLISH && !rules.needsModeration(content)) {
            publisher.approve(review, revision, mediaList);
        }

        log.info("User {} updated review {} to revision {}", userId, reviewId, newNo);
        return assembler.ownerView(review);
    }

    // =====================================================================
    // DELETE  (xóa mềm - mục 7.4 là đề xuất, cần chốt chính sách; idempotent)
    // =====================================================================

    @Override
    @Transactional
    public void deleteReview(String userId, String reviewId) {
        Review review = reviewRepository.findForUpdate(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));
        requireOwner(review, userId);
        if (review.getStatus() == ReviewStatus.DELETED) {
            return; // xóa lặp lại không lỗi
        }

        // Review đang được tính trong điểm tổng thì phải trừ ra
        boolean counted = review.getStatus() == ReviewStatus.APPROVED;
        Integer countedRating = counted ? review.getRating() : null;

        // Revision đang chờ duyệt không còn ý nghĩa: rút khỏi hàng chờ
        revisionRepository.findByReviewIdAndRevisionNo(review.getId(), review.getLatestRevisionNo())
                .filter(r -> r.getStatus() == RevisionStatus.PENDING_MODERATION)
                .ifPresent(r -> r.setStatus(RevisionStatus.SUPERSEDED));

        review.setStatus(ReviewStatus.DELETED);
        review = reviewRepository.saveAndFlush(review);

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("wasApproved", counted);
        extra.put("rating", countedRating);
        outbox.enqueue(OutboxWriter.EVENT_DELETED, review, extra);
        if (counted) {
            aggregateService.change(review.getProductId(), countedRating, null, "REVIEW_DELETED");
        }
        log.info("User {} soft-deleted review {}", userId, reviewId);
    }

    // =====================================================================
    // helpers
    // =====================================================================

    /**
     * REV-10: lấy điều kiện từ projection; chưa có thì hỏi Order (nếu bật), nếu không thì báo "đang đồng bộ".
     */
    private EligibilityProjection resolveEligibility(String lineId, String userId, LocalDateTime now) {
        EligibilityProjection e = eligibilityRepository.findByLineId(lineId).orElse(null);
        if (e == null) {
            e = syncFromOrder(lineId, userId, now);
        }
        if (!e.getUserId().equals(userId) || e.getStatus() != EligibilityStatus.ELIGIBLE) {
            metrics.submitBlocked("not_eligible");
            throw new BusinessException("REVIEW_NOT_ELIGIBLE", HttpStatus.UNPROCESSABLE_ENTITY, "Bạn chỉ có thể đánh giá sản phẩm đã nhận thành công");
        }
        if (e.getEligibleUntil().isBefore(now)) {
            metrics.submitBlocked("expired");
            throw new BusinessException("REVIEW_WINDOW_EXPIRED", HttpStatus.UNPROCESSABLE_ENTITY, "Đã quá hạn " + ELIGIBILITY_WINDOW_DAYS + " ngày để đánh giá sản phẩm này");
        }
        return e;
    }

    private EligibilityProjection syncFromOrder(String lineId, String userId, LocalDateTime now) {
        Optional<OrderLineClient.OrderLineInfo> info = orderClient.find(lineId);
        if (info.isEmpty()) {
            metrics.submitBlocked("syncing");
            throw new BusinessException("ELIGIBILITY_SYNCING", HttpStatus.CONFLICT, "Đơn hàng đang được đồng bộ, vui lòng thử lại sau ít phút");
        }
        OrderLineClient.OrderLineInfo line = info.get();
        boolean delivered = "DELIVERED".equalsIgnoreCase(line.status())
                && line.deliveredAt() != null && !line.deliveredAt().isBlank();
        if (!userId.equals(line.userId()) || !delivered) {
            metrics.submitBlocked("not_eligible");
            throw new BusinessException("REVIEW_NOT_ELIGIBLE", HttpStatus.UNPROCESSABLE_ENTITY, "Bạn chỉ có thể đánh giá sản phẩm đã nhận thành công");
        }
        LocalDateTime deliveredAt;
        try {
            deliveredAt = Times.parseUtc(line.deliveredAt());
        } catch (DateTimeException ex) {
            throw new BusinessException("ELIGIBILITY_SYNCING", HttpStatus.CONFLICT, "Chưa xác định được thời điểm giao hàng");
        }
        return eligibilityRepository.save(mapper.toProjection(line, lineId, deliveredAt));
    }

    private boolean isUnchanged(ReviewRevision latest, Integer rating, String content,
                                List<ReviewMediaRequest> media) {
        if (!Objects.equals(latest.getRating(), rating) || !Objects.equals(latest.getContent(), content)) {
            return false;
        }
        List<ReviewMedia> current = mediaRepository.findByRevisionIdOrderBySortOrderAsc(latest.getId());
        if (current.size() != media.size()) {
            return false;
        }
        for (int i = 0; i < current.size(); i++) {
            if (!current.get(i).getFileId().equals(media.get(i).getFileId())
                    || current.get(i).getMediaType() != media.get(i).getMediaType()) {
                return false;
            }
        }
        return true;
    }

    private ReviewResponse replay(IdempotencyRecord record) {
        if (record.getResultRef() == null) {
            throw new BusinessException("REQUEST_PROCESSING", HttpStatus.CONFLICT, "Yêu cầu đang được xử lý, vui lòng thử lại sau");
        }
        Review review = reviewRepository.findById(record.getResultRef())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));
        return assembler.ownerView(review);
    }

    private void requireOwner(Review review, String userId) {
        if (!Objects.equals(review.getUserId(), userId)) {
            throw new ForbiddenException("REVIEW_FORBIDDEN: Bạn không có quyền thao tác trên đánh giá này");
        }
    }


    private String requireIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw new BusinessException("IDEMPOTENCY_KEY_REQUIRED", HttpStatus.BAD_REQUEST, "Thiếu header Idempotency-Key");
        }
        String trimmed = key.trim();
        if (trimmed.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new BusinessException("IDEMPOTENCY_KEY_INVALID", HttpStatus.BAD_REQUEST,
                    "Idempotency-Key tối đa " + MAX_IDEMPOTENCY_KEY_LENGTH + " ký tự");
        }
        return trimmed;
    }

    private String fingerprint(ReviewCreateRequest r, String content) {
        StringBuilder raw = new StringBuilder()
                .append(r.getLineId()).append('|')
                .append(r.getRating()).append('|')
                .append(content).append('|')
                .append(r.getIsAnonymous() == null || r.getIsAnonymous());
        if (r.getMedia() != null) {
            r.getMedia().forEach(m -> raw.append('|').append(m.getFileId()).append(':').append(m.getMediaType()));
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash); // 64 ký tự, vừa cột fingerprint(64)
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Không thể tạo fingerprint cho request", e);
        }
    }
}