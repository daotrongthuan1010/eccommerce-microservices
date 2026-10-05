package com.dtthuan3.ecommerce.reviewservice.repository;

import com.dtthuan3.ecommerce.reviewservice.entity.ReviewMedia;
import com.dtthuan3.ecommerce.reviewservice.entity.enums.ReviewMediaType;
import com.dtthuan3.ecommerce.reviewservice.entity.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, Long> {
    void deleteByReviewId(Long reviewId);
    @Query("""
       SELECT COUNT(DISTINCT rm.reviewId)
       FROM ReviewMedia rm
       WHERE rm.mediaType = :mediaType
       AND rm.reviewId IN (
           SELECT r.id
           FROM Review r
           WHERE r.productId = :productId
           AND r.status = :status
       )
       """)
    long countReviewsWithMedia(
            @Param("productId") Long productId,
            @Param("status") ReviewStatus status,
            @Param("mediaType") ReviewMediaType mediaType
    );
}
