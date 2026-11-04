package com.dtthuan3.ecommerce.paymentservice.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PaymentStatusTest {

    @Test
    void happyPathTransitions() {
        assertTrue(PaymentStatus.CREATED.canTransitionTo(PaymentStatus.PENDING));
        assertTrue(PaymentStatus.PENDING.canTransitionTo(PaymentStatus.PAID));
        assertTrue(PaymentStatus.PAID.canTransitionTo(PaymentStatus.PARTIALLY_REFUNDED));
        assertTrue(PaymentStatus.PARTIALLY_REFUNDED.canTransitionTo(PaymentStatus.REFUNDED));
    }

    @Test
    void latePaymentIsOnlyEscapeFromTerminal() {
        assertTrue(PaymentStatus.EXPIRED.canTransitionTo(PaymentStatus.PAID));
        assertFalse(PaymentStatus.EXPIRED.canTransitionTo(PaymentStatus.PENDING));
        assertFalse(PaymentStatus.FAILED.canTransitionTo(PaymentStatus.PAID));
        assertFalse(PaymentStatus.CANCELLED.canTransitionTo(PaymentStatus.PAID));
        assertFalse(PaymentStatus.REFUNDED.canTransitionTo(PaymentStatus.PAID));
    }

    @Test
    void illegalTransitionsRejected() {
        assertFalse(PaymentStatus.PAID.canTransitionTo(PaymentStatus.CANCELLED));
        assertFalse(PaymentStatus.CREATED.canTransitionTo(PaymentStatus.PAID));
        assertFalse(PaymentStatus.PENDING.canTransitionTo(PaymentStatus.REFUNDED));
    }
}
