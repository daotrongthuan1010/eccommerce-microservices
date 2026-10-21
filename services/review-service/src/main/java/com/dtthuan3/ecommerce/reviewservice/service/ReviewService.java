package com.dtthuan3.ecommerce.reviewservice.service;

import com.dtthuan3.ecommerce.reviewservice.dto.request.ModerationRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;

public interface ReviewService {
    ReviewResponse createReview(String userId, String userName, String idempotencyKey, ReviewRequest request);
    ReviewResponse updateReview(String userId, String reviewId, ReviewRequest request);
    void deleteReview(String userId, String reviewId);
}
