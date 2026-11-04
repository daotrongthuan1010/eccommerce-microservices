package com.dtthuan3.ecommerce.shippingservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.shippingservice.dto.request.CarrierWebhookRequest;
import com.dtthuan3.ecommerce.shippingservice.dto.response.ShipmentResponse;
import com.dtthuan3.ecommerce.shippingservice.service.ShippingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook trang thai tu carrier — public (khong JWT), bao ve bang HMAC + timestamp tolerance.
 * Trang thai trung/bi lui van tra 200 de carrier dung retry (xu ly o service layer).
 */
@RestController
@RequestMapping("/shipments/webhooks")
public class ShipmentWebhookController {

    private final ShippingService shippingService;

    public ShipmentWebhookController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @PostMapping("/carrier")
    public ApiResponse<ShipmentResponse> carrierWebhook(@Valid @RequestBody CarrierWebhookRequest body) {
        return ApiResponse.ok(shippingService.handleCarrierWebhook(body));
    }
}
