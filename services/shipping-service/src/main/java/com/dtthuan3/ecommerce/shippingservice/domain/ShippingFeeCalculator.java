package com.dtthuan3.ecommerce.shippingservice.domain;

import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Bang gia noi bo (tariff) — deterministic, khong phu thuoc carrier de order du doan duoc phi.
 * - Khoi luong tinh phi = max(thuc te, quy doi D*W*H/5000 gram).
 * - 500g dau: LOCAL 16.500 / REGION 22.000 / NATIONAL 32.000 VND; moi 500g them: +5.000/+6.500/+9.000.
 * - EXPRESS x1.6. Phi lam tron xuong hang tram dong.
 */
@Component
public class ShippingFeeCalculator {

    public enum Zone { LOCAL, REGION, NATIONAL }

    private static final Set<String> NORTH = Set.of(
            "01", "02", "04", "06", "08", "10", "11", "12", "14", "15", "17", "19", "20", "22",
            "24", "25", "26", "27", "30", "31", "33", "34", "35", "36", "37", "38");
    private static final Set<String> CENTRAL = Set.of(
            "40", "42", "44", "45", "46", "48", "49", "51", "52", "54", "56", "58", "60", "62",
            "64", "66", "67", "68", "70", "72", "74", "75", "77");
    private static final Set<String> SOUTH = Set.of(
            "79", "80", "82", "83", "84", "86", "87", "89", "91", "92", "93", "94", "95", "96");

    public record FeeBreakdown(Zone zone, int billableWeightGrams, long baseFee, long weightFee, long totalFee) {}

    public FeeBreakdown calculate(String fromProvince, String toProvince, int weightGrams,
                                  Integer lengthCm, Integer widthCm, Integer heightCm, ServiceLevel level) {
        int billable = weightGrams;
        if (lengthCm != null && widthCm != null && heightCm != null) {
            int dimensional = (int) Math.ceil(lengthCm * widthCm * heightCm / 5.0); // cm^3/5 = gram quy doi (L*W*H/5000 kg)
            billable = Math.max(billable, dimensional);
        }
        Zone zone = zoneOf(fromProvince, toProvince);
        long base = switch (zone) { case LOCAL -> 16_500; case REGION -> 22_000; case NATIONAL -> 32_000; };
        long step = switch (zone) { case LOCAL -> 5_000; case REGION -> 6_500; case NATIONAL -> 9_000; };
        int extraUnits = Math.max(0, (int) Math.ceil((billable - 500) / 500.0));
        long weightFee = (long) extraUnits * step;
        long total = Math.round((base + weightFee) * level.multiplier() / 100.0) * 100;
        return new FeeBreakdown(zone, billable, base, weightFee, total);
    }

    public Zone zoneOf(String fromProvince, String toProvince) {
        if (fromProvince != null && fromProvince.equals(toProvince)) return Zone.LOCAL;
        String rf = regionOf(fromProvince);
        String rt = regionOf(toProvince);
        if (rf != null && rf.equals(rt)) return Zone.REGION;
        return Zone.NATIONAL;
    }

    private String regionOf(String provinceCode) {
        if (provinceCode == null) return null;
        if (NORTH.contains(provinceCode)) return "NORTH";
        if (CENTRAL.contains(provinceCode)) return "CENTRAL";
        if (SOUTH.contains(provinceCode)) return "SOUTH";
        return null;
    }
}
