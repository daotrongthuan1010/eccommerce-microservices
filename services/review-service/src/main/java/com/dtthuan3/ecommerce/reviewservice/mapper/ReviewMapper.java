package com.dtthuan3.ecommerce.reviewservice.mapper;

import com.dtthuan3.ecommerce.reviewservice.dto.request.ModerationRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;
import com.dtthuan3.ecommerce.reviewservice.entity.*;
import com.dtthuan3.ecommerce.reviewservice.enums.ModerationAction;
import com.dtthuan3.ecommerce.reviewservice.enums.OutboxStatus;
import com.dtthuan3.ecommerce.reviewservice.enums.ReviewStatus;
import com.dtthuan3.ecommerce.reviewservice.enums.RevisionStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(Review review) {
        if (review == null) return null;

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProductId())
                .reviewerDisplayName(review.getReviewerDisplayName())
                .status(review.getStatus() != null ? review.getStatus().name() : null)
                .revisionNo(review.getLatestRevisionNo())
                .helpfulCount(review.getHelpfulCount())
                .submittedAt(review.getSubmittedAt())
                .publishedAt(review.getPublishedAt())
                .editableUntil(review.getEditableUntil())
                .version(review.getVersion())
                .build();
    }

    public ReviewResponse toFullDetailResponse(Review review,
                                               ReviewRevision revision,
                                               List<ReviewImage> images,
                                               ReviewReply reply) {
        if (review == null) return null;

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProductId())
                .reviewerDisplayName(review.getReviewerDisplayName())
                .verifiedPurchase(true)
                .status(review.getStatus() != null ? review.getStatus().name() : null)
                .revisionNo(review.getLatestRevisionNo())
                .helpfulCount(review.getHelpfulCount())
                .submittedAt(review.getSubmittedAt())
                .publishedAt(review.getPublishedAt())
                .editableUntil(review.getEditableUntil())
                .version(review.getVersion())
                .rating(revision != null ? revision.getRating() : review.getRating())
                .content(revision != null ? revision.getContent() : null)
                .images(images == null ? List.of() : images.stream()
                        .map(ReviewImage::getFileId)
                        .toList())
                .reply(reply != null ? ReviewResponse.ReviewReplyDto.builder()
                        .content(reply.getContent())
                        .publishedAt(reply.getCreatedAt())
                        .build() : null)
                .build();
    }

    // ==========================================
    // 2. NHÓM HÀM KHỞI TẠO ENTITY (TẠO MỚI)
    // ==========================================

    public Review createReviewEntity(String userId, String finalDisplayName,
                                     EligibilityProjection eligibility,
                                     ReviewRequest req, boolean hasPhoto) {
        LocalDateTime now = LocalDateTime.now();
        return Review.builder()
                .lineId(req.getLineId())
                .orderId(eligibility.getOrderId())
                .userId(userId)
                .productId(eligibility.getProductId())
                .skuId(eligibility.getSkuId())
                .reviewerDisplayName(finalDisplayName)
                .status(ReviewStatus.PENDING_MODERATION) // Chờ duyệt
                .rating(req.getRating())
                .hasPhoto(hasPhoto)
                .latestRevisionNo(1) // Mặc định tạo mới là phiên bản 1
                .helpfulCount(0)
                .submittedAt(now)
                .editableUntil(now.plusDays(7)) // Cho phép sửa trong 7 ngày
                .build();
    }

    public ReviewRevision createInitialRevision(String reviewId, ReviewRequest req) {
        return ReviewRevision.builder()
                .reviewId(reviewId)
                .revisionNo(1)
                .rating(req.getRating())
                .content(req.getContent())
                .status(RevisionStatus.PENDING_MODERATION)
                .submittedAt(LocalDateTime.now())
                .build();
    }

    public List<ReviewImage> createReviewImages(String revisionId, List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) return List.of();

        return IntStream.range(0, fileIds.size())
                .mapToObj(i -> ReviewImage.builder()
                        .revisionId(revisionId)
                        .fileId(fileIds.get(i))
                        .sortOrder(i)
                        .build())
                .toList();
    }

    public OutboxEvent createReviewSubmittedEvent(Review review) {
        String payload = String.format("{\"reviewId\":\"%s\",\"productId\":\"%s\"}",
                review.getId(), review.getProductId());

        return OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .aggregateType("REVIEW")
                .aggregateId(review.getId())
                .aggregateVersion(1L)
                .eventType("ReviewSubmitted")
                .schemaVersion(1)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .build();
    }

    // ==========================================
    // 3. NHÓM HÀM CẬP NHẬT ENTITY (SỬA ĐÁNH GIÁ)
    // ==========================================

    public void updateReviewEntity(Review review, ReviewRequest req, boolean hasPhoto) {
        review.setRating(req.getRating());
        review.setHasPhoto(hasPhoto);
        review.setStatus(ReviewStatus.PENDING_MODERATION); // Sửa xong phải chờ duyệt lại
        review.setLatestRevisionNo(review.getLatestRevisionNo() + 1); // Tăng version hiển thị
    }

    public ReviewRevision createNewRevision(Review review, ReviewRequest req) {
        return ReviewRevision.builder()
                .reviewId(review.getId())
                .revisionNo(review.getLatestRevisionNo()) // Số revision mới nhất
                .rating(req.getRating())
                .content(req.getContent())
                .status(RevisionStatus.PENDING_MODERATION)
                .submittedAt(LocalDateTime.now())
                .build();
    }
    public OutboxEvent createReviewUpdatedEvent(Review review) {
        String payload = String.format("{\"reviewId\":\"%s\",\"productId\":\"%s\",\"revisionNo\":%d}",
                review.getId(), review.getProductId(), review.getLatestRevisionNo());

        return OutboxEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .aggregateType("REVIEW")
                .aggregateId(review.getId())
                .aggregateVersion(review.getVersion()) // Đính kèm version mới nhất
                .eventType("ReviewUpdated") // Khác với lúc Create (ReviewSubmitted)
                .schemaVersion(1)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .build();
    }
    public OutboxEvent createReviewDeletedEvent(Review review) {
        // Payload đính kèm số sao cũ để hệ thống cộng dồn (Aggregator) biết đường trừ đi
        String payload = String.format("{\"reviewId\":\"%s\",\"productId\":\"%s\",\"rating\":%d}",
                review.getId(), review.getProductId(), review.getRating());

        return OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .aggregateType("REVIEW")
                .aggregateId(review.getId())
                .aggregateVersion(review.getVersion() + 1) // Cộng version lên 1
                .eventType("ReviewDeleted") // Sự kiện báo hệ thống khác biết để trừ điểm
                .schemaVersion(1)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .build();
    }
    // ==========================================
    // 5. NHÓM HÀM KIỂM DUYỆT (MODERATION)
    // ==========================================

    // ==========================================
    // 5. NHÓM HÀM KIỂM DUYỆT (MODERATION)
    // ==========================================

    public ModerationDecision createModerationDecision(String reviewId, String revisionId,
                                                       String adminId, ModerationRequest req) {

        String rCode = req.getIsApproved() ? "MANUAL_APPROVAL" : req.getReasonCode();

        return ModerationDecision.builder()
                .reviewId(reviewId)
                .revisionId(revisionId)
                .decidedBy(adminId)
                // Đã đổi thành APPROVE và REJECT theo đúng Enum của bạn
                .decision(req.getIsApproved() ? ModerationAction.APPROVE : ModerationAction.REJECT)
                .reasonCode(rCode)
                .reasonNote(req.getReasonNote())
                .decidedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Sự kiện bắn lên Kafka sau khi kiểm duyệt xong
     */
    public OutboxEvent createReviewModeratedEvent(Review review, boolean isApproved) {
        // Nếu duyệt: báo ReviewPublished để hệ thống Rating cộng điểm
        // Nếu từ chối: báo ReviewRejected để hệ thống Notification gửi email/thông báo cho khách
        String eventType = isApproved ? "ReviewPublished" : "ReviewRejected";

        String payload = String.format("{\"reviewId\":\"%s\",\"productId\":\"%s\",\"rating\":%d}",
                review.getId(), review.getProductId(), review.getRating());

        return OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .aggregateType("REVIEW")
                .aggregateId(review.getId())
                .aggregateVersion(review.getVersion() + 1)
                .eventType(eventType)
                .schemaVersion(1)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .build();
    }
}