package com.dtthuan3.ecommerce.productservice.controller;

import com.dtthuan3.ecommerce.productservice.dto.request.ProductVariantRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.VariantAttributeValueRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductVariantResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.VariantAttributeValueResponse;
import com.dtthuan3.ecommerce.productservice.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product-variants")
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    // =========================
    // PRODUCT VARIANT
    // =========================

    @PostMapping
    public ResponseEntity<ProductVariantResponse> create(@Valid @RequestBody ProductVariantRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(productVariantService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductVariantResponse> getById(@PathVariable Long id) {

        return ResponseEntity.ok(productVariantService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductVariantResponse> update(@PathVariable Long id, @Valid @RequestBody ProductVariantRequest request) {

        return ResponseEntity.ok(productVariantService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        productVariantService.delete(id);

        return ResponseEntity.noContent().build();
    }

    // =========================
    // VARIANT ATTRIBUTE VALUE
    // =========================

    @PostMapping("/attribute-values")
    public ResponseEntity<VariantAttributeValueResponse> createAttributeValue(@Valid @RequestBody VariantAttributeValueRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(productVariantService.createAttributeValue(request));
    }

    @GetMapping("/attribute-values/{id}")
    public ResponseEntity<VariantAttributeValueResponse> getAttributeValueById(@PathVariable Long id) {

        return ResponseEntity.ok(productVariantService.getAttributeValueById(id));
    }

    @PutMapping("/attribute-values/{id}")
    public ResponseEntity<VariantAttributeValueResponse> updateAttributeValue(@PathVariable Long id, @Valid @RequestBody VariantAttributeValueRequest request) {

        return ResponseEntity.ok(productVariantService.updateAttributeValue(id, request));
    }

    @DeleteMapping("/attribute-values/{id}")
    public ResponseEntity<Void> deleteAttributeValue(@PathVariable Long id) {

        productVariantService.deleteAttributeValue(id);

        return ResponseEntity.noContent().build();
    }
}