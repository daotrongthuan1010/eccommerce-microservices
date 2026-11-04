package com.dtthuan3.ecommerce.paymentservice.repository;

import com.dtthuan3.ecommerce.paymentservice.domain.RefundStatus;
import com.dtthuan3.ecommerce.paymentservice.entity.PaymentRefund;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, String> {
    Optional<PaymentRefund> findByIdempotencyKey(String idempotencyKey);
    List<PaymentRefund> findByPaymentId(String paymentId);

    @Query("select coalesce(sum(r.amount),0) from PaymentRefund r where r.paymentId = :paymentId and r.status in :statuses")
    long sumByPaymentIdAndStatusIn(@Param("paymentId") String paymentId, @Param("statuses") List<RefundStatus> statuses);
}
