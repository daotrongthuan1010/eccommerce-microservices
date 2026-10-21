package com.dtthuan3.ecommerce.reviewservice.service;

import com.dtthuan3.ecommerce.reviewservice.dto.request.ModerationRequest;

public interface ReviewModerationService {
    void moderateReview(String moderatorId, String reviewId, ModerationRequest request);
}
