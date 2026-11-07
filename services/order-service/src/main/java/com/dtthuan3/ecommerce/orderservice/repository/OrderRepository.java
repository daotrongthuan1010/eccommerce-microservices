package com.dtthuan3.ecommerce.orderservice.repository;

import com.dtthuan3.ecommerce.orderservice.entity.Order;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, String> {
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    Optional<Order> findByIdAndUserId(String id, String userId);
    Page<Order> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    boolean existsByOrderCode(String orderCode);

    /** Admin list: loc theo status (optional), search theo orderCode/userId contains (optional). */
    @org.springframework.data.jpa.repository.Query("""
            SELECT o FROM Order o
            WHERE (:status IS NULL OR o.status = :status)
              AND (:search IS NULL OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(o.userId) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY o.createdAt DESC
            """)
    Page<Order> search(String status, String search, Pageable pageable);
}
