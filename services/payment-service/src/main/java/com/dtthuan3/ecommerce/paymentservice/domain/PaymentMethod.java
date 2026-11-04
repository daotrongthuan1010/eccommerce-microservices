package com.dtthuan3.ecommerce.paymentservice.domain;

/** Phuong thuc thanh toan v1. VNPAY/ZALOPAY di qua fake-gateway, COD thu tien khi giao hang. */
public enum PaymentMethod {
    VNPAY, ZALOPAY, COD;

    public boolean isOnline() { return this != COD; }
}
