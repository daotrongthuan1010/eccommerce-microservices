package com.dtthuan3.ecommerce.reviewservice.service.impl;

import com.dtthuan3.ecommerce.reviewservice.dto.request.ModerationRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;
import com.dtthuan3.ecommerce.reviewservice.enums.ReviewStatus;
import com.dtthuan3.ecommerce.reviewservice.entity.*;
import com.dtthuan3.ecommerce.reviewservice.enums.IdempotencyStatus;
import com.dtthuan3.ecommerce.reviewservice.enums.RevisionStatus;
import com.dtthuan3.ecommerce.reviewservice.mapper.ReviewMapper;
import com.dtthuan3.ecommerce.reviewservice.repository.*;
import com.dtthuan3.ecommerce.reviewservice.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final ReviewRevisionRepository revisionRepository;
    private final ReviewImageRepository imageRepository;
    private final EligibilityProjectionRepository eligibilityRepository;
    private final OutboxEventRepository outboxRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional
    public ReviewResponse createReview(String userId, String userName, String idempotencyKey, ReviewRequest request) {
        String fingerprint = generateFingerprint(request);
        Optional<IdempotencyRecord> idemOpt = idempotencyRepository
                .findByActorIdAndOperationTypeAndIdempotencyKey(userId, "CREATE_REVIEW", idempotencyKey);

        if (idemOpt.isPresent()) {
            IdempotencyRecord record = idemOpt.get();
            if (!record.getFingerprint().equals(fingerprint)) {
                throw new RuntimeException("IDEMPOTENCY_CONFLICT: Cùng Key nhưng nội dung khác nhau");
            }
            if (record.getStatus() == IdempotencyStatus.PROCESSING) {
                throw new RuntimeException("REQUEST_PROCESSING: Đang xử lý, vui lòng chờ");
            }
            Review existingReview = reviewRepository.findById(record.getResultRef())
                    .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy đánh giá cũ"));

            return reviewMapper.toResponse(existingReview); // Trả về kết quả cũ
        }

        IdempotencyRecord idemRecord = IdempotencyRecord.builder()
                .actorId(userId)
                .operationType("CREATE_REVIEW")
                .idempotencyKey(idempotencyKey)
                .fingerprint(fingerprint)
                .status(IdempotencyStatus.PROCESSING)
                .expiresAt(LocalDateTime.now().plusHours(72))
                .build();
        idempotencyRepository.save(idemRecord);

        EligibilityProjection eligibility = eligibilityRepository.findByLineId(request.getLineId())
                .orElseThrow(() -> new RuntimeException("REVIEW_NOT_ELIGIBLE: Đơn hàng chưa giao"));

        if (!eligibility.getUserId().equals(userId)) {
            throw new RuntimeException("REVIEW_NOT_ELIGIBLE: Bạn không có quyền đánh giá đơn này");
        }
        if (eligibility.getEligibleUntil().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("REVIEW_NOT_ELIGIBLE: Đã quá hạn 30 ngày");
        }
        if (reviewRepository.findByLineId(request.getLineId()).isPresent()) {
            throw new RuntimeException("REVIEW_ALREADY_EXISTS: Sản phẩm này đã được đánh giá");
        }

        boolean hasPhoto = request.getFileIds() != null && !request.getFileIds().isEmpty();
        if (hasPhoto && request.getFileIds().size() > 5) {
            throw new RuntimeException("VALIDATION_ERROR: Tối đa 5 ảnh đính kèm");
        }
        boolean anonymous = (request.getIsAnonymous() == null || request.getIsAnonymous());
        String finalDisplayName = anonymous ? maskDisplayName(userName) : userName.trim();

        Review review = reviewMapper.createReviewEntity(userId, finalDisplayName, eligibility, request, hasPhoto);
        review = reviewRepository.save(review);

        ReviewRevision revision = reviewMapper.createInitialRevision(review.getId(), request);
        revision = revisionRepository.save(revision);

        if (hasPhoto) {
            List<ReviewImage> images = reviewMapper.createReviewImages(revision.getId(), request.getFileIds());
            imageRepository.saveAll(images);
        }

        OutboxEvent outbox = reviewMapper.createReviewSubmittedEvent(review);
        outboxRepository.save(outbox);

        idemRecord.setStatus(IdempotencyStatus.COMPLETED);
        idemRecord.setResultRef(review.getId());
        idempotencyRepository.save(idemRecord);

        log.info("User {} created review {} for lineId {}", userId, review.getId(), request.getLineId());

        return reviewMapper.toResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(String userId, String reviewId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("REVIEW_NOT_FOUND: Không tìm thấy đánh giá"));

        if (!review.getUserId().equals(userId)) {
            throw new RuntimeException("FORBIDDEN: Bạn không có quyền sửa đánh giá này");
        }
        if (review.getEditableUntil().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("REVIEW_LOCKED: Đã quá thời hạn 7 ngày, không thể sửa đánh giá");
        }
        if (!review.getVersion().equals(request.getVersion())) {
            throw new RuntimeException("OPTIMISTIC_LOCK: Đánh giá đã được cập nhật ở một nơi khác, vui lòng tải lại trang");
        }
        boolean hasPhoto = request.getFileIds() != null && !request.getFileIds().isEmpty();
        if (hasPhoto && request.getFileIds().size() > 5) {
            throw new RuntimeException("VALIDATION_ERROR: Chỉ được đính kèm tối đa 5 ảnh");
        }
        revisionRepository.findByReviewIdAndRevisionNo(review.getId(), review.getLatestRevisionNo())
                .ifPresent(oldRevision -> {
                    oldRevision.setStatus(RevisionStatus.SUPERSEDED);
                    revisionRepository.save(oldRevision);
                });
        reviewMapper.updateReviewEntity(review, request, hasPhoto);
        review = reviewRepository.save(review);
        ReviewRevision revision = reviewMapper.createNewRevision(review, request);
        revision = revisionRepository.save(revision);
        if (hasPhoto) {
            List<ReviewImage> images = reviewMapper.createReviewImages(revision.getId(), request.getFileIds());
            imageRepository.saveAll(images);
        }
        OutboxEvent outbox = reviewMapper.createReviewUpdatedEvent(review);
        outboxRepository.save(outbox);
        log.info("User {} updated review {} to revision {}", userId, reviewId, review.getLatestRevisionNo());
        return reviewMapper.toResponse(review);
    }

    @Override
    @Transactional
    public void deleteReview(String userId, String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("REVIEW_NOT_FOUND: Không tìm thấy đánh giá"));
        if (!review.getUserId().equals(userId)) {
            throw new RuntimeException("FORBIDDEN: Bạn không có quyền xóa đánh giá này");
        }
        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new RuntimeException("ALREADY_DELETED: Đánh giá này đã bị xóa");
        }
        review.setStatus(ReviewStatus.DELETED);
        reviewRepository.save(review);
        OutboxEvent outbox = reviewMapper.createReviewDeletedEvent(review);
        outboxRepository.save(outbox);
        log.info("User {} successfully soft-deleted review {}", userId, reviewId);
    }


    private String generateFingerprint(ReviewRequest request) {
        try {
            String raw = request.getLineId() + "|" + request.getRating() + "|" + request.getContent();
            if (request.getFileIds() != null) {
                raw += "|" + String.join(",", request.getFileIds());
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi hệ thống khi băm nội dung", e);
        }
    }

    private String maskDisplayName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) return "Khách hàng";
        String name = rawName.trim();
        if (name.length() <= 2) return name.charAt(0) + "***";
        return name.charAt(0) + "***" + name.charAt(name.length() - 1);
    }
}
