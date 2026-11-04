package com.dtthuan3.ecommerce.shippingservice.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Trang thai van don (chuan carrier VN kieu GHN).
 * CREATION_FAILED: shipping da ghi nhan nhung carrier khong tao duoc van don — order retry bang key moi.
 */
public enum ShipmentStatus {
    CREATED, CREATION_FAILED, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY,
    DELIVERY_FAILED, DELIVERED, RETURNING, RETURNED, CANCELLED;

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED = Map.of(
            CREATED, EnumSet.of(PICKED_UP, CANCELLED, CREATION_FAILED, RETURNING),
            CREATION_FAILED, EnumSet.noneOf(ShipmentStatus.class),
            PICKED_UP, EnumSet.of(IN_TRANSIT, RETURNING),
            IN_TRANSIT, EnumSet.of(OUT_FOR_DELIVERY, RETURNING),
            OUT_FOR_DELIVERY, EnumSet.of(DELIVERED, DELIVERY_FAILED, RETURNING),
            DELIVERY_FAILED, EnumSet.of(OUT_FOR_DELIVERY, RETURNING),
            DELIVERED, EnumSet.noneOf(ShipmentStatus.class),
            RETURNING, EnumSet.of(RETURNED),
            RETURNED, EnumSet.noneOf(ShipmentStatus.class),
            CANCELLED, EnumSet.noneOf(ShipmentStatus.class));

    public boolean canTransitionTo(ShipmentStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == RETURNED || this == CANCELLED || this == CREATION_FAILED;
    }

    /** Thu tu tien trinh de nhan biet webhook den muon/sai thu tu (khong ap dung nhanh RETURN). */
    public int progressRank() {
        return switch (this) {
            case CREATED, CREATION_FAILED -> 0;
            case PICKED_UP -> 1;
            case IN_TRANSIT -> 2;
            case OUT_FOR_DELIVERY, DELIVERY_FAILED -> 3;
            case DELIVERED -> 4;
            case RETURNING -> 5;
            case RETURNED -> 6;
            case CANCELLED -> 0;
        };
    }
}
