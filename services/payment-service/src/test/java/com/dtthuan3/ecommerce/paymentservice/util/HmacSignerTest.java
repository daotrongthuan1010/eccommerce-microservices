package com.dtthuan3.ecommerce.paymentservice.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HmacSignerTest {

    @Test
    void signAndVerifyRoundTrip() {
        String sig = HmacSigner.sign("secret", "VNPAY.txn1.10000.00.123");
        assertTrue(HmacSigner.verify("secret", "VNPAY.txn1.10000.00.123", sig));
    }

    @Test
    void tamperedPayloadFails() {
        String sig = HmacSigner.sign("secret", "VNPAY.txn1.10000.00.123");
        assertFalse(HmacSigner.verify("secret", "VNPAY.txn1.1.00.123", sig));
        assertFalse(HmacSigner.verify("wrong-secret", "VNPAY.txn1.10000.00.123", sig));
        assertFalse(HmacSigner.verify("secret", "VNPAY.txn1.10000.00.123", null));
    }
}
