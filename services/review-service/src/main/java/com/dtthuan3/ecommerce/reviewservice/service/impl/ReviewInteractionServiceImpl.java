package com.dtthuan3.ecommerce.reviewservice.service.impl;

import com.dtthuan3.ecommerce.reviewservice.entity.HelpfulVote;
import com.dtthuan3.ecommerce.reviewservice.entity.Review;
import com.dtthuan3.ecommerce.reviewservice.enums.ReviewStatus;
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
    @Override
    public void voteReview(String userId, String reviewId, boolean isHelpful) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("REVIEW_NOT_FOUND: Không tìm thấy đánh giá"));

        if (review.getStatus() != ReviewStatus.PUBLISHED) {
            throw new RuntimeException("INVALID_STATUS: Chỉ được tương tác với đánh giá đã hiển thị");
        }

        // 2. Chặn tự vote (Không cho tự vote bài của mình)
        if (review.getUserId().equals(userId)) {
            throw new RuntimeException("VALIDATION_ERROR: Bạn không thể tự vote cho đánh giá của chính mình");
        }

        // 3. Kiểm tra lịch sử vote của User này trên bài Review này
        Optional<HelpfulVote> existingVoteOpt = helpfulVoteRepository.findByReviewIdAndUserId(reviewId, userId);

        if (existingVoteOpt.isPresent()) {
            HelpfulVote existingVote = existingVoteOpt.get();

            // TÍNH IDEMPOTENT: Nếu trạng thái DB đang giống hệt Request -> Bỏ qua, tránh gọi update rác
            if (existingVote.getHelpful().equals(isHelpful)) {
                log.info("User {} sent duplicate vote {} for review {}, ignored.", userId, isHelpful, reviewId);
                return;
            }

            // Đổi ý (Ví dụ: Đang Dislike chuyển thành Like, hoặc ngược lại)
            existingVote.setHelpful(isHelpful);
            helpfulVoteRepository.save(existingVote);

            // Cập nhật lại bộ đếm cho Review
            if (isHelpful) {
                review.setHelpfulCount(review.getHelpfulCount() + 1);
            } else {
                review.setHelpfulCount(Math.max(0, review.getHelpfulCount() - 1));
            }
        } else {
            // Lần đầu tiên vote
            HelpfulVote newVote = HelpfulVote.builder()
                    .reviewId(reviewId)
                    .userId(userId)
                    .helpful(isHelpful)
                    .build();
            helpfulVoteRepository.save(newVote);

            // Tăng biến đếm (Chỉ tính vote Helpful là có điểm)
            if (isHelpful) {
                review.setHelpfulCount(review.getHelpfulCount() + 1);
            }
        }

        // Lưu lại tổng số điểm mới của Review
        reviewRepository.save(review);
        log.info("User {} voted helpful={} for review {}", userId, isHelpful, reviewId);
    }

    @Override
    public void removeVote(String userId, String reviewId) {
        helpfulVoteRepository.findByReviewIdAndUserId(reviewId, userId).ifPresent(vote -> {
            helpfulVoteRepository.delete(vote);

            if (vote.getHelpful()) {
                reviewRepository.findById(reviewId).ifPresent(review -> {
                    review.setHelpfulCount(Math.max(0, review.getHelpfulCount() - 1));
                    reviewRepository.save(review);
                });
            }
            log.info("User {} removed vote for review {}", userId, reviewId);
        });
    }

}

