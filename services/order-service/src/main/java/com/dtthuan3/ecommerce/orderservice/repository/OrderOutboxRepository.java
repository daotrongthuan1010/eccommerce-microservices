package com.dtthuan3.ecommerce.orderservice.repository;

import com.dtthuan3.ecommerce.orderservice.entity.OrderOutbox;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, String> {
    List<OrderOutbox> findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
}
