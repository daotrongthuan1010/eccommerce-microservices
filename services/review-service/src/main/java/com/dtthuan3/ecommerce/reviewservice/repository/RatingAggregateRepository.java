package com.dtthuan3.ecommerce.reviewservice.repository;

import com.dtthuan3.ecommerce.reviewservice.entity.RatingAggregate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RatingAggregateRepository extends JpaRepository<RatingAggregate, String> {

    /** Tạo dòng rỗng nếu chưa có (an toàn khi nhiều giao dịch cùng tạo). */
    @Modifying(flushAutomatically = true)
    @Query(value = "INSERT INTO rating_aggregate (product_id, total_stars, review_count, star1_count, star2_count, "
            + "star3_count, star4_count, star5_count, aggregate_version, updated_at) "
            + "VALUES (:productId, 0, 0, 0, 0, 0, 0, 0, 0, :now) ON CONFLICT (product_id) DO NOTHING",
            nativeQuery = true)
    int insertEmptyIfAbsent(@Param("productId") String productId, @Param("now") LocalDateTime now);

    /** Khóa dòng aggregate: mọi thay đổi điểm của một sản phẩm được tuần tự hóa. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from RatingAggregate a where a.productId = :productId")
    Optional<RatingAggregate> findForUpdate(@Param("productId") String productId);
}
