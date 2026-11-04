package com.dtthuan3.ecommerce.paymentservice.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Trang thai giao dich. Chuyen sai se bi chan o service layer (409 INVALID_RESOURCE_STATE).
 * EXPIRED -> PAID la truong hop hop le duy nhat "di ra khoi terminal": IPN thanh cong
 * den sau khi het han (late payment) — order nhan event co co latePayment de quyet dinh.
 */
public enum PaymentStatus {
    CREATED, PENDING, PAID, FAILED, EXPIRED, CANCELLED, PARTIALLY_REFUNDED, REFUNDED;

    private static final java.util.Map<PaymentStatus, Set<PaymentStatus>> ALLOWED = java.util.Map.of(
            CREATED, EnumSet.of(PENDING, FAILED, CANCELLED),
            PENDING, EnumSet.of(PAID, FAILED, EXPIRED, CANCELLED),
            PAID, EnumSet.of(PARTIALLY_REFUNDED, REFUNDED),
            PARTIALLY_REFUNDED, EnumSet.of(PARTIALLY_REFUNDED, REFUNDED),
            EXPIRED, EnumSet.of(PAID),
            FAILED, EnumSet.noneOf(PaymentStatus.class),
            CANCELLED, EnumSet.noneOf(PaymentStatus.class),
            REFUNDED, EnumSet.noneOf(PaymentStatus.class));

    public boolean canTransitionTo(PaymentStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return this == FAILED || this == CANCELLED || this == REFUNDED;
    }
}
