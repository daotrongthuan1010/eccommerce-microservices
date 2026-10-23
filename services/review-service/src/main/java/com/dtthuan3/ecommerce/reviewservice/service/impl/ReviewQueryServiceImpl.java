package com.dtthuan3.ecommerce.reviewservice.service.impl;

import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewListResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewSummaryResponse;
import com.dtthuan3.ecommerce.reviewservice.entity.RatingAggregate;
import com.dtthuan3.ecommerce.reviewservice.entity.Review;
import com.dtthuan3.ecommerce.reviewservice.enums.ReviewStatus;
import com.dtthuan3.ecommerce.reviewservice.exception.NotFoundException;
import com.dtthuan3.ecommerce.reviewservice.mapper.ReviewAssembler;
import com.dtthuan3.ecommerce.reviewservice.repository.RatingAggregateRepository;
import com.dtthuan3.ecommerce.reviewservice.repository.ReviewRepository;
import com.dtthuan3.ecommerce.reviewservice.service.ReviewQueryService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewQueryServiceImpl implements ReviewQueryService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReviewRepository reviewRepository;
    private final RatingAggregateRepository aggregateRepository;
    private final ReviewAssembler assembler;

    @Override
    public ReviewResponse getReviewDetail(String reviewId, String viewerId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("REVIEW_NOT_FOUND", "Không tìm thấy đánh giá"));
        boolean owner = viewerId != null && viewerId.equals(review.getUserId());

        if (owner && review.getStatus() != ReviewStatus.DELETED) {
            return assembler.ownerView(review);
        }
        if (review.getStatus() == ReviewStatus.APPROVED) {
            return assembler.publicView(review);
        }
        // Chưa duyệt / bị ẩn / đã xóa: người khác coi như không tồn tại
        throw new NotFoundException("REVIEW_NOT_FOUND", "Không tìm thấy đánh giá");
    }

    @Override
    public PageResponse<ReviewListResponse> getProductReviews(String productId, int page, int size, Integer rating,
                                                              Boolean hasMedia, Boolean hasComment, String sort,
                                                              String viewerId) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Specification<Review> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("productId"), productId));
            predicates.add(cb.equal(root.get("status"), ReviewStatus.APPROVED));
            if (rating != null) {
                predicates.add(cb.equal(root.get("rating"), rating));
            }
            if (Boolean.TRUE.equals(hasMedia)) {
                predicates.add(cb.isTrue(root.get("hasPhoto")));
            }
            if (Boolean.TRUE.equals(hasComment)) {
                predicates.add(cb.isTrue(root.get("hasComment")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Review> result = reviewRepository.findAll(spec, PageRequest.of(safePage, safeSize, toSort(sort)));
        return PageResponse.of(assembler.listItems(result.getContent(), viewerId),
                safePage, safeSize, result.getTotalElements(), result.getTotalPages());
    }

    /** Đọc từ read model rating_aggregate (O(1)), không quét lại tập review. */
    @Override
    public ReviewSummaryResponse getProductSummary(String productId) {
        RatingAggregate aggregate = aggregateRepository.findById(productId).orElse(null);

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            distribution.put(star, aggregate == null ? 0L : (long) aggregate.star(star));
        }
        long total = aggregate == null ? 0 : aggregate.getReviewCount();
        long totalStars = aggregate == null ? 0 : aggregate.getTotalStars();
        double average = total == 0 ? 0.0 : Math.round(totalStars * 10.0 / total) / 10.0;

        return ReviewSummaryResponse.builder()
                .productId(productId)
                .totalReviews(total)
                .averageRating(average)
                .totalStars(totalStars)
                .distribution(distribution)
                .withMedia(reviewRepository.countByProductIdAndStatusAndHasPhoto(
                        productId, ReviewStatus.APPROVED, true))
                .withComment(reviewRepository.countByProductIdAndStatusAndHasComment(
                        productId, ReviewStatus.APPROVED, true))
                .aggregateVersion(aggregate == null ? 0L : aggregate.getAggregateVersion())
                .build();
    }

    @Override
    public PageResponse<ReviewResponse> getMyReviews(String userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<Review> result = reviewRepository.findByUserIdAndStatusNot(userId, ReviewStatus.DELETED,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.desc("submittedAt"), Sort.Order.desc("id"))));
        List<ReviewResponse> content = result.getContent().stream().map(assembler::ownerView).toList();
        return PageResponse.of(content, safePage, safeSize, result.getTotalElements(), result.getTotalPages());
    }

    /** Whitelist kiểu sắp xếp (không nhận tên cột tùy ý từ client). */
    private Sort toSort(String sort) {
        String key = sort == null ? "newest" : sort.toLowerCase();
        return switch (key) {
            case "helpful" -> Sort.by(Sort.Order.desc("helpfulCount"), Sort.Order.desc("publishedAt"),
                    Sort.Order.desc("id"));
            case "highest" -> Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("publishedAt"),
                    Sort.Order.desc("id"));
            case "lowest" -> Sort.by(Sort.Order.asc("rating"), Sort.Order.desc("publishedAt"),
                    Sort.Order.desc("id"));
            default -> Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));
        };
    }
}