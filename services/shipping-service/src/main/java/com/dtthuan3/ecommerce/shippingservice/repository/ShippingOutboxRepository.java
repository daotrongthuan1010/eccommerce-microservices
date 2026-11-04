package com.dtthuan3.ecommerce.shippingservice.repository;

import com.dtthuan3.ecommerce.shippingservice.entity.ShippingOutbox;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingOutboxRepository extends JpaRepository<ShippingOutbox, String> {
    List<ShippingOutbox> findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
}
