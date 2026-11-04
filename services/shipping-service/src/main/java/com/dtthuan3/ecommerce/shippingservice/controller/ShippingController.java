package com.dtthuan3.ecommerce.shippingservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.shippingservice.dto.request.CreateQuoteRequest;
import com.dtthuan3.ecommerce.shippingservice.dto.request.CreateShipmentRequest;
import com.dtthuan3.ecommerce.shippingservice.dto.response.QuoteResponse;
import com.dtthuan3.ecommerce.shippingservice.dto.response.ShipmentResponse;
import com.dtthuan3.ecommerce.shippingservice.entity.ShipmentStatusHistory;
import com.dtthuan3.ecommerce.shippingservice.service.ShippingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** API shipping cho order-service (qua gateway: /api/shipping/**, StripPrefix=2 -> /quotes, /shipments). */
@RestController
public class ShippingController {

    private final ShippingService shippingService;

    public ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @PostMapping("/quotes")
    public ResponseEntity<ApiResponse<QuoteResponse>> createQuote(@Valid @RequestBody CreateQuoteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(shippingService.createQuote(req)));
    }

    @PostMapping("/shipments")
    public ResponseEntity<ApiResponse<ShipmentResponse>> createShipment(
            @Valid @RequestBody CreateShipmentRequest req,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(shippingService.create(req, idempotencyKey)));
    }

    @GetMapping("/shipments/{id}")
    public ApiResponse<ShipmentResponse> get(@PathVariable String id) {
        return ApiResponse.ok(shippingService.get(id));
    }

    @GetMapping("/shipments/by-order/{orderId}")
    public ApiResponse<ShipmentResponse> byOrder(@PathVariable String orderId) {
        return ApiResponse.ok(shippingService.getByOrder(orderId));
    }

    @GetMapping("/shipments/{id}/history")
    public ApiResponse<List<ShipmentStatusHistory>> history(@PathVariable String id) {
        return ApiResponse.ok(shippingService.getHistory(id));
    }

    @PostMapping("/shipments/{id}/cancel")
    public ApiResponse<ShipmentResponse> cancel(@PathVariable String id) {
        return ApiResponse.ok(shippingService.cancel(id));
    }
}
