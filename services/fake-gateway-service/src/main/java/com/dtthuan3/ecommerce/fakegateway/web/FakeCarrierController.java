package com.dtthuan3.ecommerce.fakegateway.web;

import com.dtthuan3.ecommerce.fakegateway.config.FakeProperties;
import com.dtthuan3.ecommerce.fakegateway.store.FakeStore;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * FAKE carrier kieu GHN: tao van don, tra tracking, va endpoint /simulate la "bang dieu khien
 * test" — ngoai that khong ton tai (carrier that tu day webhook theo tien trinh giao hang).
 */
@RestController
@RequestMapping("/fake/carrier")
public class FakeCarrierController {

    private final FakeStore store;
    private final FakeProperties props;
    private final SignatureSupport signatures;
    private final CallbackSender callbacks;

    public FakeCarrierController(FakeStore store, FakeProperties props, SignatureSupport signatures, CallbackSender callbacks) {
        this.store = store;
        this.props = props;
        this.signatures = signatures;
        this.callbacks = callbacks;
    }

    public record CreateShipmentRequest(String clientShipmentId, String orderId, long codAmount, long shippingFee,
                                        String recipientName, String recipientPhone, String recipientAddress,
                                        String recipientProvinceCode, String webhookUrl) {}

    public record SimulateRequest(String status, String note) {}

    @PostMapping("/shipments")
    public Map<String, Object> createShipment(@RequestBody CreateShipmentRequest req,
                                              @RequestHeader("X-Timestamp") String ts,
                                              @RequestHeader("X-Signature") String signature) {
        signatures.requireValid(props.getCarrierSecret(), req.clientShipmentId() + "." + req.codAmount() + "." + ts, ts, signature);

        FakeStore.FakeCarrierShipment shipment = new FakeStore.FakeCarrierShipment();
        shipment.carrierCode = "FGHN" + store.nextCarrierSequence();
        shipment.clientShipmentId = req.clientShipmentId();
        shipment.codAmount = req.codAmount();
        shipment.shippingFee = req.shippingFee();
        shipment.webhookUrl = req.webhookUrl();
        store.putShipment(shipment);

        return Map.of("carrierCode", shipment.carrierCode, "status", shipment.status, "shippingFee", shipment.shippingFee);
    }

    @GetMapping("/shipments/{carrierCode}")
    public Map<String, Object> query(@PathVariable String carrierCode) {
        FakeStore.FakeCarrierShipment s = requireShipment(carrierCode);
        return Map.of("carrierCode", s.carrierCode, "status", s.status, "codAmount", s.codAmount);
    }

    @PostMapping("/shipments/{carrierCode}/cancel")
    public Map<String, Object> cancel(@PathVariable String carrierCode,
                                      @RequestHeader("X-Timestamp") String ts,
                                      @RequestHeader("X-Signature") String signature) {
        signatures.requireValid(props.getCarrierSecret(), carrierCode + ".CANCELLED." + ts, ts, signature);
        FakeStore.FakeCarrierShipment s = requireShipment(carrierCode);
        s.status = "CANCELLED";
        return Map.of("carrierCode", s.carrierCode, "status", s.status);
    }

    /** TEST-ONLY: day trang thai tiep theo va ban webhook ve shipping-service. */
    @PostMapping("/shipments/{carrierCode}/simulate")
    public Map<String, Object> simulate(@PathVariable String carrierCode, @RequestBody SimulateRequest req) {
        FakeStore.FakeCarrierShipment s = requireShipment(carrierCode);
        s.status = req.status().toUpperCase();
        callbacks.sendCarrierWebhook(s, req.note());
        return Map.of("carrierCode", s.carrierCode, "status", s.status, "webhookSentTo", String.valueOf(s.webhookUrl));
    }

    private FakeStore.FakeCarrierShipment requireShipment(String carrierCode) {
        FakeStore.FakeCarrierShipment s = store.shipment(carrierCode);
        if (s == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fake carrier shipment not found: " + carrierCode);
        return s;
    }
}
