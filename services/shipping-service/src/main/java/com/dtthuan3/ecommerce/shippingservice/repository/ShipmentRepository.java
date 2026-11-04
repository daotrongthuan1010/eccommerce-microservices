package com.dtthuan3.ecommerce.shippingservice.repository;

import com.dtthuan3.ecommerce.shippingservice.entity.Shipment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, String> {
    Optional<Shipment> findByIdempotencyKey(String idempotencyKey);
    Optional<Shipment> findByCarrierCode(String carrierCode);
    Optional<Shipment> findFirstByOrderIdOrderByCreatedAtDesc(String orderId);
    boolean existsByQuoteId(String quoteId);
}
