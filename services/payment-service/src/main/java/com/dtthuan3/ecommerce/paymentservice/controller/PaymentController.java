package com.dtthuan3.ecommerce.paymentservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.paymentservice.dto.request.CreatePaymentRequest;
import com.dtthuan3.ecommerce.paymentservice.dto.request.CreateRefundRequest;
import com.dtthuan3.ecommerce.paymentservice.dto.response.PaymentResponse;
import com.dtthuan3.ecommerce.paymentservice.dto.response.RefundResponse;
import com.dtthuan3.ecommerce.paymentservice.security.SecurityUtils;
import com.dtthuan3.ecommerce.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API payment cho order-service (qua gateway: /api/payment/**).
 * Moi POST tao resource deu bat buoc Idempotency-Key.
 */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> create(
            @Valid @RequestBody CreatePaymentRequest req,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        String userId = SecurityUtils.currentUser().map(u -> u.subject()).orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(paymentService.create(req, idempotencyKey, userId)));
    }

    @GetMapping("/{id}")
    public ApiResponse<PaymentResponse> get(@PathVariable String id) {
        return ApiResponse.ok(paymentService.get(id));
    }

    @GetMapping("/by-order/{orderId}")
    public ApiResponse<PaymentResponse> byOrder(@PathVariable String orderId) {
        return ApiResponse.ok(paymentService.getByOrder(orderId));
    }

    @PostMapping("/{id}/sync")
    public ApiResponse<PaymentResponse> sync(@PathVariable String id) {
        return ApiResponse.ok(paymentService.sync(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<PaymentResponse> cancel(@PathVariable String id) {
        return ApiResponse.ok(paymentService.cancel(id));
    }

    @PostMapping("/{id}/refunds")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundResponse>> refund(
            @PathVariable String id,
            @Valid @RequestBody CreateRefundRequest req,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(paymentService.refund(id, req, idempotencyKey)));
    }

    @PostMapping("/{id}/cod/collected")
    public ApiResponse<PaymentResponse> codCollected(@PathVariable String id) {
        return ApiResponse.ok(paymentService.codCollected(id));
    }

    @PostMapping("/{id}/cod/failed")
    public ApiResponse<PaymentResponse> codFailed(@PathVariable String id) {
        return ApiResponse.ok(paymentService.codFailed(id));
    }
}
