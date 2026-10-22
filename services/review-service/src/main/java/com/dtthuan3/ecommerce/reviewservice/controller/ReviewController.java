package com.dtthuan3.ecommerce.reviewservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewCreateRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewUpdateRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;
import com.dtthuan3.ecommerce.reviewservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ReviewCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        ReviewResponse data = reviewService.createReview(jwt.getSubject(), displayNameOf(jwt), idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable("id") String reviewId,
            @Valid @RequestBody ReviewUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.updateReview(jwt.getSubject(), reviewId, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable("id") String reviewId,
            @AuthenticationPrincipal Jwt jwt) {
        reviewService.deleteReview(jwt.getSubject(), reviewId);
        return ResponseEntity.ok(ApiResponse.ok());
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
