package com.dtthuan3.ecommerce.orderservice.domain;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * §ADR-0001 issue #4: OrderStatus enum + state machine — moi transition qua
 * mot cho duy nhat thay vi string literal rai rac. Gia tri enum trung khop
 * cot status VARCHAR(24) hien co (khong doi schema DB).
 *
 * <p>Spec transitions theo GLOSSARY.md:
 * PENDING_PAYMENT -> PAID | PAYMENT_FAILED | CANCELLED | EXPIRED;
 * PAID -> CONFIRMED | CANCELLED; CONFIRMED -> SHIPPED; SHIPPED -> DELIVERED.
 * PAYMENT_FAILED/CANCELLED/EXPIRED/DELIVERED la trang thai cuoi.
 */
public enum OrderStatus {
    PENDING_PAYMENT("PENDING_PAYMENT"),
    PAID("PAID"),
    CONFIRMED("CONFIRMED"),
    SHIPPED("SHIPPED"),
    DELIVERED("DELIVERED"),
    PAYMENT_FAILED("PAYMENT_FAILED"),
    CANCELLED("CANCELLED"),
    EXPIRED("EXPIRED");

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    /** Gia tri luu DB — trung string hien co de khong phai migration. */
    public String getValue() {
        return value;
    }

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            PENDING_PAYMENT, Set.of(PAID, PAYMENT_FAILED, CANCELLED, EXPIRED),
            PAID, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(SHIPPED),
            SHIPPED, Set.of(DELIVERED),
            DELIVERED, Set.of(),
            PAYMENT_FAILED, Set.of(),
            CANCELLED, Set.of(),
            EXPIRED, Set.of());

    /** Transition co duoc phep khong — nguon su that duy nhat cho tat ca guard. */
    public boolean canTransitionTo(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    /** Guard cho code duong: tu choi ro rang thay vi im lang bo qua. */
    public static void assertTransition(OrderStatus from, OrderStatus to) {
        if (!from.canTransitionTo(to)) {
            throw new IllegalStateException(
                    "Transition khong hop le: %s -> %s".formatted(from.getValue(), to.getValue()));
        }
    }

    /** Parse tu string DB; gia tri la tu choi ngay thay vi null lang le. */
    public static OrderStatus fromValue(String value) {
        return Stream.of(values())
                .filter(s -> s.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("OrderStatus khong hop le: " + value));
    }
}
