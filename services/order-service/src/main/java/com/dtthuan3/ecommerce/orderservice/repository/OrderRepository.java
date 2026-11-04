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
}
