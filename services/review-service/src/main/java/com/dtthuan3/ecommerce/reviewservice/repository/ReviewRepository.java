package com.dtthuan3.ecommerce.reviewservice.repository;

import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewResponse;
import com.dtthuan3.ecommerce.reviewservice.entity.Review;
import com.dtthuan3.ecommerce.reviewservice.entity.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByUserIdAndOrderItemId(Long userId, Long orderItemId);
    Page<Review> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);
    Page<Review> findByProductIdAndRatingAndStatus(Long productId, Integer rating, ReviewStatus status, Pageable pageable);
    Page<Review> findByUserId(Long userId,Pageable pageable);
    // Thống kê: số review theo từng mức sao (chỉ review PUBLISHED). Mỗi row = [rating, count]
    @Query("select r.rating, count(r) from Review r " +
            "where r.productId = :productId and r.status = :status " +
            "group by r.rating")
    List<Object[]> countRatingsByProductId(@Param("productId") Long productId,
                                           @Param("status") ReviewStatus status);

    // Điểm trung bình số sao (chỉ review PUBLISHED), chưa có review thì trả 0
    @Query("select coalesce(avg(r.rating), 0) from Review r " +
            "where r.productId = :productId and r.status = :status")
    Double findAverageRating(@Param("productId") Long productId,
                             @Param("status") ReviewStatus status);
}