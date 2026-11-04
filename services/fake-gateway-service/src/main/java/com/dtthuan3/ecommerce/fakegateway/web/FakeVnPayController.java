package com.dtthuan3.ecommerce.fakegateway.web;

import com.dtthuan3.ecommerce.fakegateway.config.FakeProperties;
import com.dtthuan3.ecommerce.fakegateway.store.FakeStore;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

/**
 * FAKE VNPay: tao order -> trang hosted-page cho user chon ket qua -> ban IPN -> redirect return-url.
 */
@RestController
@RequestMapping("/fake/vnpay")
public class FakeVnPayController {

    private final FakeStore store;
    private final FakeProperties props;
    private final SignatureSupport signatures;
    private final CallbackSender callbacks;

    public FakeVnPayController(FakeStore store, FakeProperties props, SignatureSupport signatures, CallbackSender callbacks) {
        this.store = store;
        this.props = props;
        this.signatures = signatures;
        this.callbacks = callbacks;
    }

    public record CreateOrderRequest(String txnRef, long amount, String returnUrl, String ipnUrl) {}

    @PostMapping("/orders")
    public Map<String, Object> createOrder(@RequestBody CreateOrderRequest req,
                                           @RequestHeader("X-Timestamp") String ts,
                                           @RequestHeader("X-Signature") String signature,
                                           @RequestHeader(value = "X-Fake-Scenario", required = false) String scenario)
            throws InterruptedException {
        signatures.requireValid(props.getVnpaySecret(), req.txnRef() + "." + req.amount() + "." + ts, ts, signature);
        if ("TIMEOUT".equalsIgnoreCase(String.valueOf(scenario))) Thread.sleep(6000);

        FakeStore.FakePaymentOrder order = new FakeStore.FakePaymentOrder();
        order.txnRef = req.txnRef();
        order.provider = "VNPAY";
        order.amount = req.amount();
        order.returnUrl = req.returnUrl();
        order.ipnUrl = req.ipnUrl();
        order.scenario = scenario == null ? "" : scenario;
        store.putPayment(order);

        return Map.of("txnRef", order.txnRef, "status", order.status,
                "payUrl", props.getPublicBaseUrl() + "/fake/vnpay/pay?txnRef=" + encode(order.txnRef));
    }

    @GetMapping(value = "/pay", produces = MediaType.TEXT_HTML_VALUE)
    public String payPage(@RequestParam String txnRef) {
        FakeStore.FakePaymentOrder order = requireOrder(txnRef);
        return """
                <!doctype html><html><head><meta charset="utf-8"><title>FAKE VNPay</title></head>
                <body style="font-family:sans-serif;max-width:520px;margin:40px auto">
                <h2>FAKE VNPay — cong thanh toan gia lap</h2>
                <p>Ma giao dich: <b>%s</b></p><p>So tien: <b>%d VND</b></p>
                <p>Kich ban loi dang ap dung: <b>%s</b></p>
                <form method="post" action="/fake/vnpay/pay/%s/complete">
                  <button name="outcome" value="SUCCESS" style="padding:10px 18px">Thanh toan thanh cong</button>
                  <button name="outcome" value="FAILED" style="padding:10px 18px">Thanh toan that bai / huy</button>
                </form>
                <p style="color:#888">Trang nay chi la gia lap de hoc/test — khong phai VNPay that.</p>
                </body></html>
                """.formatted(order.txnRef, order.amount, order.scenario.isBlank() ? "(mac dinh)" : order.scenario, encode(order.txnRef));
    }

    @PostMapping("/pay/{txnRef}/complete")
    public ResponseEntity<Void> complete(@PathVariable String txnRef, @RequestParam String outcome) {
        FakeStore.FakePaymentOrder order = requireOrder(txnRef);
        boolean success = "SUCCESS".equalsIgnoreCase(outcome);
        order.status = success ? "SUCCESS" : "FAILED";
        order.resultCode = success ? "00" : "24";
        callbacks.sendPaymentCallback(order);

        String redirect = (order.returnUrl == null || order.returnUrl.isBlank())
                ? props.getPublicBaseUrl() + "/fake/vnpay/pay?txnRef=" + encode(txnRef)
                : order.returnUrl + (order.returnUrl.contains("?") ? "&" : "?")
                        + "txnRef=" + encode(txnRef) + "&resultCode=" + order.resultCode;
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirect)).build();
    }

    @GetMapping("/orders/{txnRef}")
    public Map<String, Object> queryOrder(@PathVariable String txnRef,
                                          @RequestHeader("X-Timestamp") String ts,
                                          @RequestHeader("X-Signature") String signature) {
        signatures.requireValid(props.getVnpaySecret(), txnRef + ".0." + ts, ts, signature);
        FakeStore.FakePaymentOrder order = requireOrder(txnRef);
        return Map.of("txnRef", order.txnRef, "amount", order.amount, "status", order.status, "resultCode", order.resultCode);
    }

    @PostMapping("/refunds")
    public Map<String, Object> refund(@RequestBody Map<String, Object> req,
                                      @RequestHeader("X-Timestamp") String ts,
                                      @RequestHeader("X-Signature") String signature) {
        String txnRef = String.valueOf(req.get("txnRef"));
        long amount = req.get("amount") instanceof Number n ? n.longValue() : 0L;
        signatures.requireValid(props.getVnpaySecret(), txnRef + "." + amount + "." + ts, ts, signature);
        return Map.of("refundRef", "FAKE-VNPAY-RF-" + txnRef, "status", "SUCCEEDED");
    }

    private FakeStore.FakePaymentOrder requireOrder(String txnRef) {
        FakeStore.FakePaymentOrder order = store.payment("VNPAY", txnRef);
        if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fake VNPay order not found: " + txnRef);
        return order;
    }

    private static String encode(String v) {
        return UriUtils.encodeQueryParam(v, StandardCharsets.UTF_8);
    }
}
