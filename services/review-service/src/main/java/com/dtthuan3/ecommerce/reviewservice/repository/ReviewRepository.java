package com.dtthuan3.ecommerce.reviewservice.repository;

import com.dtthuan3.ecommerce.reviewservice.entity.Review;
import com.dtthuan3.ecommerce.reviewservice.enums.ReviewStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String>, JpaSpecificationExecutor<Review> {

    /** Một dòng đơn hàng chỉ có một đánh giá. */
    Optional<Review> findByLineId(String lineId);

    /** "Đánh giá của tôi" (không gồm bài đã xóa). */
    Page<Review> findByUserIdAndStatusNot(String userId, ReviewStatus status, Pageable pageable);

    long countByProductIdAndStatusAndHasPhoto(String productId, ReviewStatus status, Boolean hasPhoto);

    long countByProductIdAndStatusAndHasComment(String productId, ReviewStatus status, Boolean hasComment);

    /** Phân bố sao: mỗi phần tử là [Integer rating, Long count]. */
    @Query("select r.rating, count(r) from Review r "
            + "where r.productId = :productId and r.status = :status group by r.rating")
    List<Object[]> countByRating(@Param("productId") String productId, @Param("status") ReviewStatus status);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Review r where r.id = :id")
    Optional<Review> findForUpdate(@Param("id") String id);

    @Modifying(flushAutomatically = true)
    @Query("update Review r set r.helpfulCount = :count where r.id = :id")
    int setHelpfulCount(@Param("id") String id, @Param("count") int count);
}
