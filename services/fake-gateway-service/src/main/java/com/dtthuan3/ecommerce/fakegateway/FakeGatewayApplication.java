package com.dtthuan3.ecommerce.fakegateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FAKE payment gateway + fake carrier: process rieng, secret rieng, in-memory.
 * Mo phong VNPay (hosted page + IPN), ZaloPay (callback) va carrier kieu GHN (webhook trang thai)
 * kem cac kich ban loi: DROP_IPN, DUPLICATE_IPN, BAD_SIGNATURE, AMOUNT_TAMPER, TIMEOUT.
 */
@SpringBootApplication
public class FakeGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(FakeGatewayApplication.class, args);
    }
}
