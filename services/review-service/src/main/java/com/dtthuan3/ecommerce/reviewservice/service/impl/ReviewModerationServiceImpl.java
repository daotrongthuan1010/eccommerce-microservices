package com.dtthuan3.ecommerce.reviewservice.service.impl;

import com.dtthuan3.ecommerce.reviewservice.dto.request.ModerationRequest;
import com.dtthuan3.ecommerce.reviewservice.entity.ModerationDecision;
import com.dtthuan3.ecommerce.reviewservice.entity.OutboxEvent;
import com.dtthuan3.ecommerce.reviewservice.entity.Review;
import com.dtthuan3.ecommerce.reviewservice.entity.ReviewRevision;
import com.dtthuan3.ecommerce.reviewservice.enums.ReviewStatus;
import com.dtthuan3.ecommerce.reviewservice.enums.RevisionStatus;
import com.dtthuan3.ecommerce.reviewservice.mapper.ReviewMapper;
import com.dtthuan3.ecommerce.reviewservice.repository.*;
import com.dtthuan3.ecommerce.reviewservice.service.ReviewModerationService;
import com.dtthuan3.ecommerce.reviewservice.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewModerationServiceImpl implements ReviewModerationService {
    private final ReviewRepository reviewRepository;
    private final ReviewRevisionRepository revisionRepository;
    private final OutboxEventRepository outboxRepository;
    private final ModerationDecisionRepository moderationDecisionRepository;
    private final ReviewMapper reviewMapper;
    @Override
    public void moderateReview(String moderatorId, String reviewId, ModerationRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("REVIEW_NOT_FOUND: Không tìm thấy đánh giá"));

        if (review.getStatus() != ReviewStatus.PENDING_MODERATION) {
            throw new RuntimeException("INVALID_STATUS: Đánh giá này không ở trạng thái chờ kiểm duyệt");
        }

        if (!request.getIsApproved() && (request.getReasonCode() == null || request.getReasonCode().trim().isEmpty())) {
            throw new RuntimeException("VALIDATION_ERROR: Vui lòng nhập mã lý do từ chối");
        }

        ReviewRevision revision = revisionRepository
                .findByReviewIdAndRevisionNo(review.getId(), review.getLatestRevisionNo())
                .orElseThrow(() -> new RuntimeException("Lỗi dữ liệu: Không tìm thấy nội dung của đánh giá"));
        LocalDateTime now = LocalDateTime.now();
        if (request.getIsApproved()) {
            review.setStatus(ReviewStatus.PUBLISHED);
            review.setPublishedAt(now);
            revision.setStatus(RevisionStatus.APPROVED);
        } else {
            review.setStatus(ReviewStatus.REJECTED);
            revision.setStatus(RevisionStatus.REJECTED);
        }
        reviewRepository.save(review);
        revisionRepository.save(revision);
        ModerationDecision decision = reviewMapper.createModerationDecision(
                review.getId(), revision.getId(), moderatorId, request);
        moderationDecisionRepository.save(decision);
        OutboxEvent outbox = reviewMapper.createReviewModeratedEvent(review, request.getIsApproved());
        outboxRepository.save(outbox);
        log.info("Moderator {} {} review {}", moderatorId, request.getIsApproved() ? "approved" : "rejected", reviewId);
    }

}
