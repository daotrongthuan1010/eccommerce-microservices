package com.dtthuan3.ecommerce.shippingservice.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ShippingFeeCalculatorTest {

    private final ShippingFeeCalculator calc = new ShippingFeeCalculator();

    @Test
    void sameProvinceIsLocalBasePrice() {
        var fee = calc.calculate("79", "79", 400, null, null, null, ServiceLevel.STANDARD);
        assertEquals(ShippingFeeCalculator.Zone.LOCAL, fee.zone());
        assertEquals(16_500, fee.totalFee());
    }

    @Test
    void sameRegionAndCrossRegionZones() {
        // Ha Noi (01) -> Hai Phong (31): cung mien Bac
        assertEquals(ShippingFeeCalculator.Zone.REGION, calc.zoneOf("01", "31"));
        // Ha Noi (01) -> HCM (79): khac mien
        assertEquals(ShippingFeeCalculator.Zone.NATIONAL, calc.zoneOf("01", "79"));
        // Tinh la khong co trong bang: mac dinh NATIONAL cho chac
        assertEquals(ShippingFeeCalculator.Zone.NATIONAL, calc.zoneOf("99", "01"));
    }

    @Test
    void extraWeightAddsSteps() {
        // 1.200g di quoc gia: 32.000 + 2 step * 9.000 = 50.000
        var fee = calc.calculate("01", "79", 1_200, null, null, null, ServiceLevel.STANDARD);
        assertEquals(50_000, fee.totalFee());
    }

    @Test
    void dimensionalWeightWinsWhenLarger() {
        // 40x40x40cm -> 12.800g quy doi, nang hon 1.000g thuc
        var fee = calc.calculate("79", "79", 1_000, 40, 40, 40, ServiceLevel.STANDARD);
        assertEquals(12_800, fee.billableWeightGrams());
    }

    @Test
    void expressMultipliesPrice() {
        var standard = calc.calculate("79", "79", 400, null, null, null, ServiceLevel.STANDARD);
        var express = calc.calculate("79", "79", 400, null, null, null, ServiceLevel.EXPRESS);
        assertEquals(Math.round(standard.totalFee() * 1.6 / 100.0) * 100, express.totalFee());
    }
}
