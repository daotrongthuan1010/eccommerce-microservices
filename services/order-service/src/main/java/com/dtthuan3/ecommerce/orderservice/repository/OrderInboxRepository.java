package com.dtthuan3.ecommerce.orderservice.repository;

import com.dtthuan3.ecommerce.orderservice.entity.OrderInbox;
import com.dtthuan3.ecommerce.orderservice.entity.OrderInbox.InboxKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderInboxRepository extends JpaRepository<OrderInbox, InboxKey> {
    boolean existsByEventIdAndSource(String eventId, String source);
}
