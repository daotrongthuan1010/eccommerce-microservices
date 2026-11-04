package com.dtthuan3.ecommerce.shippingservice.dto.response;

import com.dtthuan3.ecommerce.shippingservice.domain.ShipmentStatus;
import com.dtthuan3.ecommerce.shippingservice.entity.Shipment;
import java.time.Instant;

public record ShipmentResponse(
        String id,
        String orderId,
        String carrier,
        String carrierCode,
        ShipmentStatus status,
        long shippingFee,
        long codAmount,
        String currency,
        int deliveryAttempts,
        Instant createdAt,
        Instant updatedAt) {

    public static ShipmentResponse from(Shipment s) {
        return new ShipmentResponse(s.getId(), s.getOrderId(), s.getCarrier(), s.getCarrierCode(), s.getStatus(),
                s.getShippingFee(), s.getCodAmount(), s.getCurrency(), s.getDeliveryAttempts(),
                s.getCreatedAt(), s.getUpdatedAt());
    }
}
