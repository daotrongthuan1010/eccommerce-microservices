package com.dtthuan3.ecommerce.reviewservice.service.impl;

import com.dtthuan3.ecommerce.reviewservice.dto.request.ReportReviewRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.HelpfulResponse;
import com.dtthuan3.ecommerce.reviewservice.entity.AbuseReport;
import com.dtthuan3.ecommerce.reviewservice.entity.HelpfulVote;
import com.dtthuan3.ecommerce.reviewservice.entity.Review;
import com.dtthuan3.ecommerce.reviewservice.enums.ReportStatus;
import com.dtthuan3.ecommerce.reviewservice.enums.ReviewStatus;
import com.dtthuan3.ecommerce.reviewservice.exception.ConflictException;
import com.dtthuan3.ecommerce.reviewservice.exception.NotFoundException;
import com.dtthuan3.ecommerce.reviewservice.exception.UnprocessableEntityException;
import com.dtthuan3.ecommerce.reviewservice.repository.AbuseReportRepository;
import com.dtthuan3.ecommerce.reviewservice.repository.HelpfulVoteRepository;
import com.dtthuan3.ecommerce.reviewservice.repository.ReviewRepository;
import com.dtthuan3.ecommerce.reviewservice.service.ReviewInteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewInteractionServiceImpl implements ReviewInteractionService {
    private final ReviewRepository reviewRepository;
    private final HelpfulVoteRepository helpfulVoteRepository;
    private final AbuseReportRepository abuseReportRepository;
    @Override
    public HelpfulResponse setHelpful(String userId, String reviewId, boolean helpful) {
        Review review = reviewRepository.findForUpdate(reviewId)
                .filter(r -> r.getStatus() == ReviewStatus.APPROVED) // HIDDEN/PENDING... không nhận vote
                .orElseThrow(() -> new NotFoundException("REVIEW_NOT_FOUND: Không tìm thấy đánh giá"));
        if (review.getUserId().equals(userId)) {
            throw new UnprocessableEntityException("SELF_VOTE_NOT_ALLOWED: Bạn không thể bấm hữu ích cho đánh giá của chính mình");
        }

        HelpfulVote vote = helpfulVoteRepository.findByReviewIdAndUserId(reviewId, userId).orElse(null);
        if (vote == null) {
            helpfulVoteRepository.save(HelpfulVote.builder()
                    .reviewId(reviewId)
                    .userId(userId)
                    .helpful(helpful)
                    .build());
        } else if (!vote.getHelpful().equals(helpful)) {
            vote.setHelpful(helpful);
            helpfulVoteRepository.save(vote);
        }

        long count = helpfulVoteRepository.countByReviewIdAndHelpfulTrue(reviewId);
        reviewRepository.setHelpfulCount(reviewId, (int) count);
        return HelpfulResponse.builder()
                .reviewId(reviewId)
                .helpfulCount(count)
                .helpfulByCurrentUser(helpful)
                .build();
    }
    @Override
    public void report(String userId, String reviewId, ReportReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .filter(r -> r.getStatus() == ReviewStatus.APPROVED)
                .orElseThrow(() -> new NotFoundException("REVIEW_NOT_FOUND: Không tìm thấy đánh giá"));
        if (review.getUserId().equals(userId)) {
            throw new UnprocessableEntityException("SELF_REPORT_NOT_ALLOWED: Bạn không thể báo cáo đánh giá của chính mình");
        }
        if (abuseReportRepository.existsByReviewIdAndReporterId(reviewId, userId)) {
            throw new ConflictException("REPORT_ALREADY_EXISTS: Bạn đã báo cáo đánh giá này rồi");
        }
        abuseReportRepository.save(AbuseReport.builder()
                .reviewId(reviewId)
                .reporterId(userId)
                .reasonCode(request.getReasonCode().trim())
                .note(request.getNote())
                .status(ReportStatus.OPEN)
                .build());
        log.info("User {} reported review {} ({})", userId, reviewId, request.getReasonCode());
    }
}

