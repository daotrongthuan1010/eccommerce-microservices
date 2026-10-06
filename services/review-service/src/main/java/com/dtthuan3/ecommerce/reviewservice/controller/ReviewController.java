package com.dtthuan3.ecommerce.reviewservice.controller;

import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewCreateRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewUpdateRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.SellerReplyRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.HelpfulResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewListResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewSummaryResponse;
import com.dtthuan3.ecommerce.reviewservice.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping("/product/{prodcutId}")
    public Page<ReviewListResponse> getReviewByProduct(@PathVariable Long prodcutId,
                                                       @RequestParam(required = false) Integer rating,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "5") int size) {
        return reviewService.getReviewByProduct(prodcutId, rating, page, size);
    }

    @GetMapping("/my-review")
    public Page<ReviewResponse> getReviewByUser(@RequestParam Long userId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "5") int size) {
        return reviewService.getMyReview(userId, page, size);
    }

    @PostMapping("/create")
    public ReviewResponse createReview(@RequestParam Long userId,
                                       @RequestBody ReviewCreateRequest request) {
        return reviewService.createReview(userId, request);
    }

    @PutMapping("/update/{reviewId}")
    public ReviewResponse updateReview(@PathVariable Long reviewId,
                                       @RequestParam Long userId,
                                       @RequestBody ReviewUpdateRequest request) {

        return reviewService.updateReview(reviewId, userId, request);
    }

    @DeleteMapping("/delete/{reviewId}")
    public void deleteReview(@PathVariable Long reviewId, @RequestParam Long userId) {
        reviewService.deleteReview(reviewId, userId);
    }

    @GetMapping("/product/{productId}/summary")
    public ReviewSummaryResponse getReviewSummary(@PathVariable Long productId) {
        return reviewService.getReviewSummary(productId);
    }

    @PostMapping("/{reviewId}/reply")
    public ReviewResponse replyReview(@PathVariable Long reviewId,
                                      @RequestBody SellerReplyRequest request) {
        return reviewService.replyReview(reviewId, request);
    }

    @PostMapping("/{reviewId}/helpful")
    public HelpfulResponse toggleHelpful(@PathVariable Long reviewId,
                                         @RequestParam Long userId) {
        return reviewService.toggleHelpful(reviewId, userId);
    }

    @GetMapping("/{reviewId}")
    public ReviewResponse getReviewById(@PathVariable Long reviewId) {
        return reviewService.getReviewById(reviewId);
    }

}
