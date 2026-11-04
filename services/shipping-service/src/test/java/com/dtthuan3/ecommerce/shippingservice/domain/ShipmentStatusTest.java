package com.dtthuan3.ecommerce.shippingservice.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShipmentStatusTest {

    @Test
    void happyPath() {
        assertTrue(ShipmentStatus.CREATED.canTransitionTo(ShipmentStatus.PICKED_UP));
        assertTrue(ShipmentStatus.PICKED_UP.canTransitionTo(ShipmentStatus.IN_TRANSIT));
        assertTrue(ShipmentStatus.IN_TRANSIT.canTransitionTo(ShipmentStatus.OUT_FOR_DELIVERY));
        assertTrue(ShipmentStatus.OUT_FOR_DELIVERY.canTransitionTo(ShipmentStatus.DELIVERED));
    }

    @Test
    void cancelOnlyBeforePickup() {
        assertTrue(ShipmentStatus.CREATED.canTransitionTo(ShipmentStatus.CANCELLED));
        assertFalse(ShipmentStatus.PICKED_UP.canTransitionTo(ShipmentStatus.CANCELLED));
        assertFalse(ShipmentStatus.DELIVERED.canTransitionTo(ShipmentStatus.CANCELLED));
    }

    @Test
    void deliveryFailedCanRetryOrReturn() {
        assertTrue(ShipmentStatus.DELIVERY_FAILED.canTransitionTo(ShipmentStatus.OUT_FOR_DELIVERY));
        assertTrue(ShipmentStatus.DELIVERY_FAILED.canTransitionTo(ShipmentStatus.RETURNING));
        assertTrue(ShipmentStatus.RETURNING.canTransitionTo(ShipmentStatus.RETURNED));
        assertFalse(ShipmentStatus.RETURNED.canTransitionTo(ShipmentStatus.DELIVERED));
    }
}
