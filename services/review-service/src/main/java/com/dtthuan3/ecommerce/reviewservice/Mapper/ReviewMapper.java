package com.dtthuan3.ecommerce.reviewservice.Mapper;

import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewCreateRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.*;

import com.dtthuan3.ecommerce.reviewservice.entity.Review;

import com.dtthuan3.ecommerce.reviewservice.entity.enums.ReviewStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReviewMapper {
    public Review toEntity(
            ReviewCreateRequest request,
            Long userId,
            Long orderId,
            Long productId,
            Long variantId
    ) {
        if (request == null) {
            return null;
        }

        return Review.builder()
                .userId(userId)
                .orderId(orderId)
                .orderItemId(request.getOrderItemId())
                .productId(productId)
                .variantId(variantId)
                .rating(request.getRating())
                .comment(
                        normalizeComment(
                                request.getComment()
                        )
                )
                .anonymous(
                        Boolean.TRUE.equals(
                                request.getAnonymous()
                        )
                )
                .verifiedPurchase(true)
                .helpfulCount(0L)
                .status(ReviewStatus.PUBLISHED)
                .edited(false)
                .build();
    }
    public ReviewResponse toResponse(Review review) {
        if (review == null) {
            return null;
        }
        String displayName;

        if (review.isAnonymous()) {
            displayName = "Người dùng ẩn danh";
        } else {
            displayName = "User " + review.getUserId();
        }
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .displayUserName(displayName)
                .orderId(review.getOrderId())
                .orderItemId(review.getOrderItemId())
                .productId(review.getProductId())
                .variantId(review.getVariantId())
                .rating(review.getRating())
                .comment(review.getComment())
                .verifiedPurchase(review.isVerifiedPurchase())
                .anonymous(review.isAnonymous())
                .helpfulCount(review.getHelpfulCount())
                .sellerReply(review.getSellerReply())
                .sellerRepliedAt(review.getSellerRepliedAt())
                .status(review.getStatus())
                .edited(review.isEdited())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }


    public ReviewListResponse toListResponse(Review review) {
        if (review == null) {
            return null;
        }

        String displayName;

        if (review.isAnonymous()) {
            displayName = "Người dùng ẩn danh";
        } else {
            displayName = "User " + review.getUserId();
        }

        return ReviewListResponse.builder()
                .id(review.getId())
                .displayUserName(displayName)
                .rating(review.getRating())
                .comment(review.getComment())
                .verifiedPurchase(review.isVerifiedPurchase())
                .anonymous(review.isAnonymous())
                .helpfulCount(review.getHelpfulCount())
                .sellerReply(review.getSellerReply())
                .createdAt(review.getCreatedAt())
                .build();
    }
    public ReviewListResponse toListResponse(Review review, List<ReviewMediaResponse> media) {
        if (review == null) {
            return null;
        }
        String displayName;
        if (review.isAnonymous()) {
            displayName = "Người dùng ẩn danh";
        } else {
            displayName = "User " + review.getUserId();
        }

        return ReviewListResponse.builder()
                .id(review.getId())
                .displayUserName(displayName)
                .rating(review.getRating())
                .comment(review.getComment())
                .verifiedPurchase(review.isVerifiedPurchase())
                .anonymous(review.isAnonymous())
                .helpfulCount(review.getHelpfulCount())
                .sellerReply(review.getSellerReply())
                .media(media)
                .createdAt(review.getCreatedAt())
                .build();
    }



    private String normalizeComment(String comment) {
        if (comment == null) {
            return null;
        }
        String value = comment.trim();
        if (value.isEmpty()) {
            return null;
        }
        return value;
    }
    public HelpfulResponse toHelpfulResponse(Long reviewId, Long helpfulCount, boolean helpfulByCurrentUser) {
        return HelpfulResponse.builder()
                .reviewId(reviewId)
                .helpfulCount(helpfulCount)
                .helpfulByCurrentUser(helpfulByCurrentUser)
                .build();
    }
    public ReviewSummaryResponse toSummaryResponse(
            Long productId,
            long[] stars,
            double averageRating,
            long withImages,
            long withVideos
    ) {
        long totalReviews =
                stars[1]
                        + stars[2]
                        + stars[3]
                        + stars[4]
                        + stars[5];

        return ReviewSummaryResponse.builder()
                .productId(productId)
                .totalReviews(totalReviews)
                .averageRating(
                        Math.round(
                                averageRating * 100.0
                        ) / 100.0
                )
                .oneStar(stars[1])
                .twoStars(stars[2])
                .threeStars(stars[3])
                .fourStars(stars[4])
                .fiveStars(stars[5])
                .withImages(withImages)
                .withVideos(withVideos)
                .build();
    }
}