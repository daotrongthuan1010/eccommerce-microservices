package com.dtthuan3.ecommerce.reviewservice.repository;

import com.dtthuan3.ecommerce.reviewservice.entity.ReviewRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRevisionRepository extends JpaRepository<ReviewRevision, String> {
    Optional<ReviewRevision> findByReviewIdAndRevisionNo(String reviewId, Integer revisionNo);
}
