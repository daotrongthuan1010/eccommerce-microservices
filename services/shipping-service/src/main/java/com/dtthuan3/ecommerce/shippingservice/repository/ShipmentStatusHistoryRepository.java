package com.dtthuan3.ecommerce.shippingservice.repository;

import com.dtthuan3.ecommerce.shippingservice.entity.ShipmentStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentStatusHistoryRepository extends JpaRepository<ShipmentStatusHistory, String> {
    List<ShipmentStatusHistory> findByShipmentIdOrderByOccurredAtAsc(String shipmentId);
}
