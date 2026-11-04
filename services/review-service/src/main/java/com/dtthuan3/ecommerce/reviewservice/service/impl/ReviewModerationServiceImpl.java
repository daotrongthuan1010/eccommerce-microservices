package com.dtthuan3.ecommerce.reviewservice.service.impl;


import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.common.exception.ResourceNotFoundException;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.reviewservice.util.OutboxWriter;
import com.dtthuan3.ecommerce.reviewservice.util.RatingAggregateService;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ModerationActionRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ModerationRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.SellerReplyRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.AbuseReportResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReconcileResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;
import com.dtthuan3.ecommerce.reviewservice.entity.*;
import com.dtthuan3.ecommerce.reviewservice.enums.*;
import com.dtthuan3.ecommerce.reviewservice.mapper.ReviewAssembler;
import com.dtthuan3.ecommerce.reviewservice.mapper.ReviewMapper;
import com.dtthuan3.ecommerce.reviewservice.repository.*;
import com.dtthuan3.ecommerce.reviewservice.service.ReviewModerationService;
import com.dtthuan3.ecommerce.reviewservice.util.ReviewPublisher;
import com.dtthuan3.ecommerce.reviewservice.util.ReviewRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewModerationServiceImpl implements ReviewModerationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewRepository reviewRepository;
    private final ReviewRevisionRepository revisionRepository;
    private final ReviewMediaRepository mediaRepository;
    private final ReviewReplyRepository replyRepository;
    private final AbuseReportRepository reportRepository;
    private final ModerationDecisionRepository decisionRepository;
    private final ReviewPublisher publisher;
    private final RatingAggregateService aggregateService;
    private final OutboxWriter outbox;
    private final ReviewAssembler assembler;
    private final ReviewMapper mapper;


    @Override
    @Transactional
    public PageResponse<ReviewResponse> getPendingQueue(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<ReviewRevision> revisions = revisionRepository.findByStatusOrderBySubmittedAtAsc(
                RevisionStatus.PENDING_MODERATION, PageRequest.of(safePage, safeSize));

        List<ReviewResponse> content = new ArrayList<>();
        for (ReviewRevision revision : revisions.getContent()) {
            reviewRepository.findById(revision.getReviewId())
                    .ifPresent(r -> content.add(assembler.moderationView(r, revision)));
        }
        return PageResponse.of(content, safePage, safeSize, revisions.getTotalElements(), revisions.getTotalPages());
    }

    @Override
    @Transactional
    public void decideRevision(String moderatorId, String revisionId, ModerationRequest request) {
// Lấy reviewId rồi KHÓA review trước khi đọc revision: tránh ghi đè khi chủ review đang sửa
        String reviewId = revisionRepository.findReviewIdById(revisionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nội dung đánh giá"));
        Review review = lockReview(reviewId);
        ReviewRevision revision = revisionRepository.findById(revisionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nội dung đánh giá"));

        if (review.getStatus() == ReviewStatus.DELETED || review.getStatus() == ReviewStatus.HIDDEN) {
            throw new BusinessException("REVIEW_NOT_MODERATABLE", HttpStatus.CONFLICT, "Đánh giá đã bị xóa hoặc đang bị ẩn");
        }
        if (revision.getStatus() != RevisionStatus.PENDING_MODERATION) {
            throw new BusinessException("REVISION_NOT_PENDING", HttpStatus.CONFLICT, "Nội dung này không còn ở trạng thái chờ kiểm duyệt (có thể đã được xử lý hoặc bị thay thế)");
        }

        boolean approved = Boolean.TRUE.equals(request.getIsApproved());
        if (!approved && !ReviewRules.hasText(request.getReasonCode())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Vui lòng nhập mã lý do từ chối");
        }

        if (approved) {
            List<ReviewMedia> media = mediaRepository.findByRevisionIdOrderBySortOrderAsc(revision.getId());
            publisher.approve(review, revision, media);
        } else {
            revision.setStatus(RevisionStatus.REJECTED);
            revision.setModeratedAt(LocalDateTime.now());
            revisionRepository.save(revision);
            // Đã có bản APPROVED trước đó thì giữ nguyên, chỉ bản sửa bị từ chối
            if (review.getPublishedRevisionId() == null) {
                review.setStatus(ReviewStatus.REJECTED);
            }
            review = reviewRepository.saveAndFlush(review);

            Map<String, Object> extra = new LinkedHashMap<>();
            extra.put("revisionId", revision.getId());
            extra.put("revisionNo", revision.getRevisionNo());
            extra.put("reasonCode", request.getReasonCode().trim());
            outbox.enqueue(OutboxWriter.EVENT_REJECTED, review, extra);
        }

        decisionRepository.save(ModerationDecision.builder()
                .reviewId(review.getId())
                .revisionId(revision.getId())
                .decidedBy(moderatorId)
                .decision(approved ? ModerationAction.APPROVE : ModerationAction.REJECT)
                .reasonCode(approved ? "MANUAL_APPROVAL" : request.getReasonCode().trim())
                .reasonNote(request.getReasonNote())
                .decidedAt(LocalDateTime.now())
                .build());
        log.info("Moderator {} {} revision {} of review {}", moderatorId, approved ? "approved" : "rejected",
                revisionId, review.getId());
    }

    @Override
    @Transactional
    public void hideReview(String moderatorId, String reviewId, ModerationActionRequest request) {
        Review review = lockReview(reviewId);
        if (review.getStatus() != ReviewStatus.APPROVED || review.getPublishedRevisionId() == null) {
            throw new BusinessException("REVIEW_NOT_APPROVED", HttpStatus.CONFLICT, "Chỉ ẩn được đánh giá đang ở trạng thái APPROVED");
        }
        Integer countedRating = review.getRating();

        review.setStatus(ReviewStatus.HIDDEN);
        review = reviewRepository.saveAndFlush(review);

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("rating", countedRating);
        extra.put("reasonCode", request.getReasonCode().trim());
        outbox.enqueue(OutboxWriter.EVENT_HIDDEN, review, extra);
        aggregateService.change(review.getProductId(), countedRating, null, "REVIEW_HIDDEN");

        recordDecision(moderatorId, review, ModerationAction.HIDE, request);
        resolveReport(moderatorId, request.getAbuseReportId(), review.getId(), ReportStatus.RESOLVED);
        log.info("Moderator {} hid review {}", moderatorId, reviewId);
    }

    @Override
    @Transactional
    public void restoreReview(String moderatorId, String reviewId, ModerationActionRequest request) {
        Review review = lockReview(reviewId);
        if (review.getStatus() != ReviewStatus.HIDDEN || review.getPublishedRevisionId() == null) {
            throw new BusinessException("REVIEW_NOT_HIDDEN", HttpStatus.CONFLICT, "Chỉ khôi phục được đánh giá đang bị ẩn");
        }

        review.setStatus(ReviewStatus.APPROVED);
        review = reviewRepository.saveAndFlush(review);

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("rating", review.getRating());
        extra.put("reasonCode", request.getReasonCode().trim());
        outbox.enqueue(OutboxWriter.EVENT_RESTORED, review, extra);
        aggregateService.change(review.getProductId(), null, review.getRating(), "REVIEW_RESTORED");

        // Quyết định sai được sửa bằng decision MỚI, không xóa lịch sử
        recordDecision(moderatorId, review, ModerationAction.RESTORE, request);
        log.info("Moderator {} restored review {}", moderatorId, reviewId);
    }

    @Override
    public PageResponse<AbuseReportResponse> getReports(String status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        ReportStatus reportStatus;
        try {
            reportStatus = status == null || status.isBlank() ? ReportStatus.OPEN
                    : ReportStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "status không hợp lệ");
        }
        Page<AbuseReport> reports = reportRepository.findByStatusOrderByCreatedAtAsc(
                reportStatus, PageRequest.of(safePage, safeSize));
        List<AbuseReportResponse> content = reports.getContent().stream().map(mapper::toReportResponse).toList();
        return PageResponse.of(content, safePage, safeSize, reports.getTotalElements(), reports.getTotalPages());
    }


    @Override
    @Transactional
    public void dismissReport(String moderatorId, String reportId) {
        AbuseReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo"));
        if (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.DISMISSED) {
            return; // idempotent
        }
        report.setStatus(ReportStatus.DISMISSED);
        report.setHandledBy(moderatorId);
        report.setResolvedAt(LocalDateTime.now());
        reportRepository.save(report);
    }

    @Override
    @Transactional
    public ReviewResponse.ReviewReplyDto replyToReview(String staffId, String reviewId, SellerReplyRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));
        if (review.getStatus() != ReviewStatus.APPROVED) {
            throw new BusinessException("REVIEW_NOT_APPROVED", HttpStatus.CONFLICT, "Chỉ phản hồi được đánh giá đang hiển thị");
        }
        ReviewReply reply = replyRepository.save(ReviewReply.builder()
                .reviewId(reviewId)
                .staffId(staffId)
                .content(request.getReply().trim())
                .status(ReplyStatus.PUBLISHED)
                .build());
        log.info("Staff {} replied to review {}", staffId, reviewId);
        return mapper.toReplyDto(reply);
    }

    @Override
    @Transactional
    public void removeReply(String staffId, String reviewId, String replyId) {
        ReviewReply reply = replyRepository.findById(replyId)
                .filter(r -> r.getReviewId().equals(reviewId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phản hồi"));
        reply.setStatus(ReplyStatus.REMOVED);
        replyRepository.save(reply);
        log.info("Staff {} removed reply {} of review {}", staffId, replyId, reviewId);
    }

    @Override
    public ReconcileResponse reconcile(String productId, boolean repair) {
        return null;
    }
    private Review lockReview(String reviewId) {
        return reviewRepository.findForUpdate(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));
    }

    private void recordDecision(String moderatorId, Review review, ModerationAction action,
                                ModerationActionRequest request) {
        decisionRepository.save(ModerationDecision.builder()
                .reviewId(review.getId())
                .revisionId(review.getPublishedRevisionId())
                .abuseReportId(blankToNull(request.getAbuseReportId()))
                .decidedBy(moderatorId)
                .decision(action)
                .reasonCode(request.getReasonCode().trim())
                .reasonNote(request.getReasonNote())
                .decidedAt(LocalDateTime.now())
                .build());
    }

    private void resolveReport(String moderatorId, String reportId, String reviewId, ReportStatus status) {
        if (reportId == null || reportId.isBlank()) {
            return;
        }
        reportRepository.findById(reportId)
                .filter(r -> r.getReviewId().equals(reviewId))
                .ifPresent(r -> {
                    r.setStatus(status);
                    r.setHandledBy(moderatorId);
                    r.setResolvedAt(LocalDateTime.now());
                    reportRepository.save(r);
                });
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

