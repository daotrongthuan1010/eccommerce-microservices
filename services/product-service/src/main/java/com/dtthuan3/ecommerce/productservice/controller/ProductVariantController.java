package com.dtthuan3.ecommerce.productservice.controller;

import com.dtthuan3.ecommerce.productservice.domain.ProductVariant;
import com.dtthuan3.ecommerce.productservice.dto.request.CreateProductVariantRequest;
import com.dtthuan3.ecommerce.productservice.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products-variant")
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    @PostMapping("/products/{productId}/variants")
    public ResponseEntity<ProductVariant> create(
            @PathVariable UUID productId,
            @Valid @RequestBody CreateProductVariantRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productVariantService.create(productId, request));
    }

    @GetMapping("/products/{productId}/variants")
    public ResponseEntity<List<ProductVariant>> getByProduct(
            @PathVariable UUID productId
    ) {
        return ResponseEntity.ok(
                productVariantService.getByProductId(productId)
        );
    }

    @GetMapping("/variants/{variantId}")
    public ResponseEntity<ProductVariant> getById(
            @PathVariable UUID variantId
    ) {
        return ResponseEntity.ok(
                productVariantService.getById(variantId)
        );
    }
}