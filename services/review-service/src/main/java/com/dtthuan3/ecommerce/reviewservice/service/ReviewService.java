package com.dtthuan3.ecommerce.reviewservice.service;

import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewCreateRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewUpdateRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;

public interface ReviewService {
    ReviewResponse createReview(String userId, String userName, String idempotencyKey, ReviewCreateRequest request);
    ReviewResponse updateReview(String userId, String reviewId, ReviewUpdateRequest request);
    void deleteReview(String userId, String reviewId);
}
