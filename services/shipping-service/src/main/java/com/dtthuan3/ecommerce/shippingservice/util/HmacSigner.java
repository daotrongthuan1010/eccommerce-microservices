package com.dtthuan3.ecommerce.shippingservice.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Ky/kiem HMAC-SHA256 cho trao doi server-to-server voi fake carrier (mo phong webhook GHN). */
public final class HmacSigner {

    private HmacSigner() {}

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

    public static String sha256Hex(String input) {
        try {
            byte[] raw = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot compute SHA-256", e);
        }
    }
}
