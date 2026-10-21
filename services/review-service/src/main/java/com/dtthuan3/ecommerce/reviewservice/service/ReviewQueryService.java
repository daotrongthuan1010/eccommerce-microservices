package com.dtthuan3.ecommerce.reviewservice.service;

import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;

public interface ReviewQueryService {
    ReviewResponse getReviewDetail(String reviewId);
}
