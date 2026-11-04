package com.dtthuan3.ecommerce.fakegateway.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** HMAC-SHA256 dung chung cho ca 3 fake provider (mo phong chu ky VNPay/ZaloPay/GHN). */
public final class FakeHmac {

    private FakeHmac() {}

    public static String sign(String secret, String canonical) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot compute HMAC", e);
        }
    }

    public static boolean verify(String secret, String canonical, String expectedHex) {
        if (expectedHex == null) return false;
        String actual = sign(secret, canonical);
        return MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8),
                expectedHex.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
