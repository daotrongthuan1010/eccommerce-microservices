package com.dtthuan3.ecommerce.paymentservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.paymentservice.dto.request.ProviderCallbackRequest;
import com.dtthuan3.ecommerce.paymentservice.dto.response.PaymentResponse;
import com.dtthuan3.ecommerce.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IPN/callback tu gateway — endpoint public duy nhat (khong JWT), bao ve bang chu ky HMAC
 * + timestamp tolerance 5 phut + dedup event_key. Day la nguon su that duy nhat ve ket qua
 * thanh toan; redirect return-url cua trinh duyet khong bao gio duoc tin.
 */
@RestController
@RequestMapping("/payments/webhooks")
public class PaymentWebhookController {

    private final PaymentService paymentService;

    public PaymentWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{provider}")
    public ApiResponse<PaymentResponse> callback(
            @PathVariable String provider, @Valid @RequestBody ProviderCallbackRequest body) {
        return ApiResponse.ok(paymentService.handleCallback(provider, body));
    }
}
