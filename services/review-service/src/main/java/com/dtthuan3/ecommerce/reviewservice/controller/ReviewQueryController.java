package com.dtthuan3.ecommerce.reviewservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewListResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewSummaryResponse;
import com.dtthuan3.ecommerce.reviewservice.service.ReviewQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewQueryController {
    private final ReviewQueryService queryService;

    @GetMapping("/reviews/me")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.ok(queryService.getMyReviews(jwt.getSubject(), page, size)));
    }

    @GetMapping("/reviews/{id}")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReview(
            @PathVariable("id") String reviewId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.ok(queryService.getReviewDetail(reviewId, subjectOf(jwt))));
    }

    /** Lọc: rating=1..5, hasMedia, hasComment; sort=newest|helpful|highest|lowest. Chỉ review APPROVED. */
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewListResponse>>> getProductReviews(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean hasMedia,
            @RequestParam(required = false) Boolean hasComment,
            @RequestParam(defaultValue = "newest") String sort,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.ok(queryService.getProductReviews(
                productId, page, size, rating, hasMedia, hasComment, sort, subjectOf(jwt))));
    }

    /** Điểm trung bình + phân bố sao, đọc từ rating_aggregate. */
    @GetMapping("/products/{productId}/rating")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getProductRating(@PathVariable String productId) {
        return ResponseEntity.ok(ApiResponse.ok(queryService.getProductSummary(productId)));
    }

    private static String subjectOf(Jwt jwt) {
        return jwt == null ? null : jwt.getSubject();
    }

    private static String displayNameOf(Jwt jwt) {
        if (jwt.hasClaim("name")) {
            return jwt.getClaimAsString("name");
        }
        if (jwt.hasClaim("preferred_username")) {
            return jwt.getClaimAsString("preferred_username");
        }
        return "Khách hàng";
    }
}

