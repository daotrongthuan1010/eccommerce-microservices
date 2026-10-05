package com.dtthuan3.ecommerce.reviewservice.repository;

import com.dtthuan3.ecommerce.reviewservice.entity.ReviewHelpful;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewHelpfulRepository extends JpaRepository<ReviewHelpful, Long> {
    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);

    void deleteByReviewIdAndUserId(Long reviewId, Long userId);

    long countByReviewId(Long reviewId);
}
