package com.dtthuan3.ecommerce.productservice.controller;

import com.dtthuan3.ecommerce.productservice.domain.Sku;
import com.dtthuan3.ecommerce.productservice.dto.request.CreateSkuRequest;
import com.dtthuan3.ecommerce.productservice.service.SkuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/skus")
public class SkuController {

    private final SkuService skuService;

    @PostMapping("/variants/{variantId}/skus")
    public ResponseEntity<Sku> create(
            @PathVariable UUID variantId,
            @Valid @RequestBody CreateSkuRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(skuService.create(variantId, request));
    }

    @GetMapping("/skus/{skuId}")
    public ResponseEntity<Sku> getById(
            @PathVariable UUID skuId
    ) {
        return ResponseEntity.ok(skuService.getById(skuId));
    }
}