package com.dtthuan3.ecommerce.reviewservice.service;

import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewCreateRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewUpdateRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.request.SellerReplyRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.HelpfulResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewListResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewSummaryResponse;
import org.springframework.data.domain.Page;

public interface ReviewService {
    //lấy review của 1 sản phẩm
    Page<ReviewListResponse> getReviewByProduct(Long productId, Integer rating, int page,int size);
    //lấy reivew của user đang login
    Page<ReviewResponse> getMyReview(Long userId, int page, int size);
    //tạo mới review
    ReviewResponse createReview(Long userId, ReviewCreateRequest request);
    //update review
    ReviewResponse updateReview(Long reviewId,Long userId, ReviewUpdateRequest request);
    //xoá review
    void deleteReview(Long reviewId,Long userId);
    //detail review
    ReviewResponse getReviewById(Long reviewId);
    //thông kê đánh giá
    ReviewSummaryResponse getReviewSummary(Long productId);
    //shop trả lời review
    ReviewResponse replyReview(Long reviewId, SellerReplyRequest request);
    // user bấm helpful
    HelpfulResponse toggleHelpful(Long reviewId,Long userId);
}
