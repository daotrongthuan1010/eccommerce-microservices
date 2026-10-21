package com.dtthuan3.ecommerce.reviewservice.service;

public interface ReviewInteractionService {
    void voteReview(String userId, String reviewId, boolean isHelpful);
    void removeVote(String userId, String reviewId);
}
