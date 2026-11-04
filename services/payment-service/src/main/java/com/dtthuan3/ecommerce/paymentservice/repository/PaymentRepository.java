package com.dtthuan3.ecommerce.paymentservice.repository;

import com.dtthuan3.ecommerce.paymentservice.domain.PaymentStatus;
import com.dtthuan3.ecommerce.paymentservice.entity.Payment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByProviderTxnRef(String providerTxnRef);
    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(String orderId);
    boolean existsByOrderIdAndStatusIn(String orderId, List<PaymentStatus> statuses);
    List<Payment> findByStatusAndExpiresAtBefore(PaymentStatus status, Instant cutoff);
    List<Payment> findByStatusAndUpdatedAtBefore(PaymentStatus status, Instant cutoff);
}
