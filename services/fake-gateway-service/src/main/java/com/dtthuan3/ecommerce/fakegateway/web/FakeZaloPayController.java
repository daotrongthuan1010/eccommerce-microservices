package com.dtthuan3.ecommerce.fakegateway.web;

import com.dtthuan3.ecommerce.fakegateway.config.FakeProperties;
import com.dtthuan3.ecommerce.fakegateway.store.FakeStore;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
 * FAKE ZaloPay: khong hosted-page — tra qrContent/orderUrl, ket qua chi ve bang callback
 * server-to-server (dung endpoint /complete de gia lap user quet QR xong).
 */
@RestController
@RequestMapping("/fake/zalopay")
public class FakeZaloPayController {

    private final FakeStore store;
    private final FakeProperties props;
    private final SignatureSupport signatures;
    private final CallbackSender callbacks;

    public FakeZaloPayController(FakeStore store, FakeProperties props, SignatureSupport signatures, CallbackSender callbacks) {
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
        signatures.requireValid(props.getZalopaySecret(), req.txnRef() + "." + req.amount() + "." + ts, ts, signature);
        if ("TIMEOUT".equalsIgnoreCase(String.valueOf(scenario))) Thread.sleep(6000);

        FakeStore.FakePaymentOrder order = new FakeStore.FakePaymentOrder();
        order.txnRef = req.txnRef();
        order.provider = "ZALOPAY";
        order.amount = req.amount();
        order.returnUrl = req.returnUrl();
        order.ipnUrl = req.ipnUrl();
        order.scenario = scenario == null ? "" : scenario;
        store.putPayment(order);

        return Map.of("txnRef", order.txnRef, "status", order.status,
                "orderUrl", props.getPublicBaseUrl() + "/fake/zalopay/pay?txnRef=" + encode(order.txnRef),
                "qrContent", "FAKEZALOPAY://pay?txnRef=" + order.txnRef + "&amount=" + order.amount);
    }

    @GetMapping(value = "/pay", produces = MediaType.TEXT_HTML_VALUE)
    public String payPage(@RequestParam String txnRef) {
        FakeStore.FakePaymentOrder order = requireOrder(txnRef);
        return """
                <!doctype html><html><head><meta charset="utf-8"><title>FAKE ZaloPay</title></head>
                <body style="font-family:sans-serif;max-width:520px;margin:40px auto">
                <h2>FAKE ZaloPay — gia lap quet QR</h2>
                <p>Ma giao dich: <b>%s</b></p><p>So tien: <b>%d VND</b></p>
                <form method="post" action="/fake/zalopay/orders/%s/complete">
                  <button name="outcome" value="SUCCESS" style="padding:10px 18px">Quet QR thanh cong</button>
                  <button name="outcome" value="FAILED" style="padding:10px 18px">Thanh toan that bai</button>
                </form>
                <p style="color:#888">Chi la gia lap de hoc/test.</p>
                </body></html>
                """.formatted(order.txnRef, order.amount, encode(order.txnRef));
    }

    @PostMapping("/orders/{txnRef}/complete")
    public Map<String, Object> complete(@PathVariable String txnRef, @RequestParam String outcome) {
        FakeStore.FakePaymentOrder order = requireOrder(txnRef);
        boolean success = "SUCCESS".equalsIgnoreCase(outcome);
        order.status = success ? "SUCCESS" : "FAILED";
        order.resultCode = success ? "1" : "-49";
        // Luu y: ZaloPay that dung resultCode=1 la thanh cong. Payment-service map ca "SUCCESS" status
        // lan resultCode, nen o day gui status ro rang de adapter fake doc khong phu thuoc ma so.
        if (success) order.resultCode = "00";
        callbacks.sendPaymentCallback(order);
        return Map.of("txnRef", order.txnRef, "status", order.status);
    }

    @GetMapping("/orders/{txnRef}")
    public Map<String, Object> queryOrder(@PathVariable String txnRef,
                                          @RequestHeader("X-Timestamp") String ts,
                                          @RequestHeader("X-Signature") String signature) {
        signatures.requireValid(props.getZalopaySecret(), txnRef + ".0." + ts, ts, signature);
        FakeStore.FakePaymentOrder order = requireOrder(txnRef);
        return Map.of("txnRef", order.txnRef, "amount", order.amount, "status", order.status, "resultCode", order.resultCode);
    }

    @PostMapping("/refunds")
    public Map<String, Object> refund(@RequestBody Map<String, Object> req,
                                      @RequestHeader("X-Timestamp") String ts,
                                      @RequestHeader("X-Signature") String signature) {
        String txnRef = String.valueOf(req.get("txnRef"));
        long amount = req.get("amount") instanceof Number n ? n.longValue() : 0L;
        signatures.requireValid(props.getZalopaySecret(), txnRef + "." + amount + "." + ts, ts, signature);
        return Map.of("refundRef", "FAKE-ZALOPAY-RF-" + txnRef, "status", "SUCCEEDED");
    }

    private FakeStore.FakePaymentOrder requireOrder(String txnRef) {
        FakeStore.FakePaymentOrder order = store.payment("ZALOPAY", txnRef);
        if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fake ZaloPay order not found: " + txnRef);
        return order;
    }

    private static String encode(String v) {
        return UriUtils.encodeQueryParam(v, StandardCharsets.UTF_8);
    }
}
