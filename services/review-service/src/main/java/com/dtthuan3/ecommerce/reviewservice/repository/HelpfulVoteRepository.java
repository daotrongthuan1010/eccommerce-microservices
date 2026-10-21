package com.dtthuan3.ecommerce.reviewservice.repository;

import com.dtthuan3.ecommerce.reviewservice.entity.HelpfulVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HelpfulVoteRepository extends JpaRepository<HelpfulVote, String> {
    Optional<HelpfulVote> findByReviewIdAndUserId(String reviewId, String userId);
}
