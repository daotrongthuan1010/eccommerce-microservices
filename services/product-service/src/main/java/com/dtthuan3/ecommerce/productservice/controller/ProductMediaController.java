package com.dtthuan3.ecommerce.productservice.controller;

import com.dtthuan3.ecommerce.productservice.dto.request.ProductMediaRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.SkuMediaRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductMediaResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.SkuMediaResponse;
import com.dtthuan3.ecommerce.productservice.service.ProductMediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class ProductMediaController {

    private final ProductMediaService productMediaService;

    // =========================
    // PRODUCT MEDIA
    // =========================

    @PostMapping("/products")
    public ResponseEntity<ProductMediaResponse> createProductMedia(@Valid @RequestBody ProductMediaRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(productMediaService.createProductMedia(request));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductMediaResponse> getProductMediaById(@PathVariable Long id) {

        return ResponseEntity.ok(productMediaService.getProductMediaById(id));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ProductMediaResponse> updateProductMedia(@PathVariable Long id, @Valid @RequestBody ProductMediaRequest request) {

        return ResponseEntity.ok(productMediaService.updateProductMedia(id, request));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProductMedia(@PathVariable Long id) {

        productMediaService.deleteProductMedia(id);

        return ResponseEntity.noContent().build();
    }

    // =========================
    // SKU MEDIA
    // =========================

    @PostMapping("/skus")
    public ResponseEntity<SkuMediaResponse> createSkuMedia(@Valid @RequestBody SkuMediaRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(productMediaService.createSkuMedia(request));
    }

    @GetMapping("/skus/{id}")
    public ResponseEntity<SkuMediaResponse> getSkuMediaById(@PathVariable Long id) {

        return ResponseEntity.ok(productMediaService.getSkuMediaById(id));
    }

    @PutMapping("/skus/{id}")
    public ResponseEntity<SkuMediaResponse> updateSkuMedia(@PathVariable Long id, @Valid @RequestBody SkuMediaRequest request) {

        return ResponseEntity.ok(productMediaService.updateSkuMedia(id, request));
    }

    @DeleteMapping("/skus/{id}")
    public ResponseEntity<Void> deleteSkuMedia(@PathVariable Long id) {

        productMediaService.deleteSkuMedia(id);

        return ResponseEntity.noContent().build();
    }
}