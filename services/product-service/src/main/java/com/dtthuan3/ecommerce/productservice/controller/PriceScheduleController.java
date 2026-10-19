
package com.dtthuan3.ecommerce.productservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.productservice.dto.request.CreatePriceScheduleRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.PriceScheduleResponse;
import com.dtthuan3.ecommerce.productservice.service.PriceScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/skus/{skuId}/price-schedules")
public class PriceScheduleController {

    private final PriceScheduleService priceScheduleService;

    @PostMapping
    public ResponseEntity<ApiResponse<PriceScheduleResponse>> create(
            @PathVariable UUID skuId,
            @Valid @RequestBody CreatePriceScheduleRequest request
    ) {
        PriceScheduleResponse response =
                priceScheduleService.create(skuId, request);

        return ResponseEntity
                .status(201)
                .body(ApiResponse.created(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PriceScheduleResponse>>> getBySkuId(
            @PathVariable UUID skuId
    ) {
        List<PriceScheduleResponse> response =
                priceScheduleService.getBySkuId(skuId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
