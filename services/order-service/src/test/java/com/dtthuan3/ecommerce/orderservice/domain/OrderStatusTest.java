package com.dtthuan3.ecommerce.orderservice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ORDER-UC01 (issue #4, ADR-0001): OrderStatus phai la enum + state machine
 * — moi transition qua mot cho duy nhat, transition khong hop le phai tu choi.
 * Truoc fix: string literal "PENDING_PAYMENT"/"PAYMENT_FAILED" rai rac,
 * khong ai chan chuyen trang thai sai.
 *
 * <p>Spec transitions (GLOSSARY.md: PENDING_PAYMENT -> PAID -> CONFIRMED ->
 * SHIPPED -> DELIVERED; nhanh loi PAYMENT_FAILED/CANCELLED/EXPIRED).
 */
class OrderStatusTest {

    @Test
    @DisplayName("ORDER-UC01: transition hop le theo GLOSSARY")
    void validTransitions() {
        assertThat(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.PAID)).isTrue();
        assertThat(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.PAYMENT_FAILED)).isTrue();
        assertThat(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.SHIPPED)).isTrue();
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED)).isTrue();
    }

    @Test
    @DisplayName("ORDER-UC02: transition khong hop le bi tu choi")
    void invalidTransitions() {
        // Don da PAID khong the quay lai PENDING_PAYMENT hay nhay thang DELIVERED
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.PENDING_PAYMENT)).isFalse();
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.DELIVERED)).isFalse();
        // Don FAIL khong the thanh PAID (payment retry phai tao don moi)
        assertThat(OrderStatus.PAYMENT_FAILED.canTransitionTo(OrderStatus.PAID)).isFalse();
        // Don DELIVERED la trang thai cuoi
        assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.PAID)).isFalse();
    }

    @Test
    @DisplayName("ORDER-UC03: fromValue parse dung + sai gia tri tu choi ro rang")
    void parseRoundTrip() {
        assertThat(OrderStatus.fromValue("PENDING_PAYMENT")).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(OrderStatus.PENDING_PAYMENT.getValue()).isEqualTo("PENDING_PAYMENT");
        assertThatThrownBy(() -> OrderStatus.fromValue("KHONG_TON_TAI"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ORDER-UC04: assertTransition tu choi sai voi thong bo ro rang")
    void assertTransitionThrowsWithMessage() {
        assertThatThrownBy(() -> OrderStatus.assertTransition(OrderStatus.PAID, OrderStatus.DELIVERED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PAID")
                .hasMessageContaining("DELIVERED");
    }
}
