package com.dtthuan3.ecommerce.reviewservice.service;

import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewListResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewSummaryResponse;
import org.springframework.data.domain.Page;

public interface ReviewQueryService {
    ReviewResponse getReviewDetail(String reviewId, String viewerId);

    PageResponse<ReviewListResponse> getProductReviews(String productId, int page, int size, Integer rating,
                                                       Boolean hasMedia, Boolean hasComment, String sort,
                                                       String viewerId);

    ReviewSummaryResponse getProductSummary(String productId);

    PageResponse<ReviewResponse> getMyReviews(String userId, int page, int size);
}
