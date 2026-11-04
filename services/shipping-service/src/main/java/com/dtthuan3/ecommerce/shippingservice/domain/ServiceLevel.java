package com.dtthuan3.ecommerce.shippingservice.domain;

public enum ServiceLevel {
    STANDARD(1.0), EXPRESS(1.6);

    private final double multiplier;

    ServiceLevel(double multiplier) { this.multiplier = multiplier; }

    public double multiplier() { return multiplier; }
}
