package com.dtthuan3.ecommerce.paymentservice.repository;

import com.dtthuan3.ecommerce.paymentservice.entity.PaymentCallbackLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCallbackLogRepository extends JpaRepository<PaymentCallbackLog, String> {
    boolean existsByEventKey(String eventKey);
}
