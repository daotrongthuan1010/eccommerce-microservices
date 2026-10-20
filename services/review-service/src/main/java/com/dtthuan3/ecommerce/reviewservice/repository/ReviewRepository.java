package com.dtthuan3.ecommerce.reviewservice.repository;

import com.dtthuan3.ecommerce.reviewservice.entity.Review;
import com.dtthuan3.ecommerce.reviewservice.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {
    // Phục vụ kiểm tra: 1 dòng đơn hàng (orderLine) chỉ có 1 review
    Optional<Review> findByLineId(String lineId);

    // Phục vụ màn hình Public (Khách vào xem sản phẩm)
    Page<Review> findByProductIdAndStatus(String productId, ReviewStatus status, Pageable pageable);

    // Phục vụ màn hình "Đánh giá của tôi" (My Reviews)
    Page<Review> findByUserId(String userId, Pageable pageable);
}
