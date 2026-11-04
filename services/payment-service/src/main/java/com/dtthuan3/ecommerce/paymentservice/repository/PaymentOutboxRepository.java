package com.dtthuan3.ecommerce.paymentservice.repository;

import com.dtthuan3.ecommerce.paymentservice.entity.PaymentOutbox;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, String> {
    List<PaymentOutbox> findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
}
