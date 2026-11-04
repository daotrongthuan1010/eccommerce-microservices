package com.dtthuan3.ecommerce.fakegateway.store;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/** Luu trang thai fake trong RAM (mat khi restart — chap nhan duoc voi cong gia lap). */
@Component
public class FakeStore {

    public static class FakePaymentOrder {
        public String txnRef;
        public String provider;
        public long amount;
        public String returnUrl;
        public String ipnUrl;
        public String scenario = "";
        public String status = "PENDING"; // PENDING | SUCCESS | FAILED
        public String resultCode = "";
    }

    public static class FakeCarrierShipment {
        public String carrierCode;
        public String clientShipmentId;
        public long codAmount;
        public long shippingFee;
        public String webhookUrl;
        public String status = "CREATED";
    }

    private final Map<String, FakePaymentOrder> payments = new ConcurrentHashMap<>();
    private final Map<String, FakeCarrierShipment> shipments = new ConcurrentHashMap<>();
    // Epoch-second giu nguyen qua restart trong vong ~50 nam; counter con lai
    // (1e9/s) du dai de khong trung: tranh duplicate carrier_code khi fake-gateway
    // restart mat RAM nhung shipping-service van giu ma cu trong DB.
    private final AtomicLong carrierSequence = new AtomicLong(Instant.now().getEpochSecond());

    public void putPayment(FakePaymentOrder order) { payments.put(order.provider + ":" + order.txnRef, order); }
    public FakePaymentOrder payment(String provider, String txnRef) { return payments.get(provider + ":" + txnRef); }

    public void putShipment(FakeCarrierShipment shipment) { shipments.put(shipment.carrierCode, shipment); }
    public FakeCarrierShipment shipment(String carrierCode) { return shipments.get(carrierCode); }
    public long nextCarrierSequence() { return carrierSequence.incrementAndGet(); }
}
