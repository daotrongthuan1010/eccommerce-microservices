package com.dtthuan3.ecommerce.productservice.controller;

import com.dtthuan3.ecommerce.productservice.dto.request.ProductAttributeValueRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.ProductRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductAttributeValueResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductResponse;
import com.dtthuan3.ecommerce.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // =========================
    // PRODUCT
    // =========================

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {

        return ResponseEntity.ok(productService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {

        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        productService.delete(id);

        return ResponseEntity.noContent().build();
    }

    // =========================
    // PRODUCT STATUS
    // =========================

    @PostMapping("/{id}/submit")
    public ResponseEntity<ProductResponse> submitForApproval(@PathVariable Long id) {

        return ResponseEntity.ok(productService.submitForApproval(id));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ProductResponse> activate(@PathVariable Long id) {

        return ResponseEntity.ok(productService.activate(id));
    }

    @PostMapping("/{id}/discontinue")
    public ResponseEntity<ProductResponse> discontinue(@PathVariable Long id) {

        return ResponseEntity.ok(productService.discontinue(id));
    }

    // =========================
    // PRODUCT ATTRIBUTE VALUE
    // =========================

    @PostMapping("/attribute-values")
    public ResponseEntity<ProductAttributeValueResponse> createAttributeValue(@Valid @RequestBody ProductAttributeValueRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createAttributeValue(request));
    }

    @GetMapping("/attribute-values/{id}")
    public ResponseEntity<ProductAttributeValueResponse> getAttributeValueById(@PathVariable Long id) {

        return ResponseEntity.ok(productService.getAttributeValueById(id));
    }

    @PutMapping("/attribute-values/{id}")
    public ResponseEntity<ProductAttributeValueResponse> updateAttributeValue(@PathVariable Long id, @Valid @RequestBody ProductAttributeValueRequest request) {

        return ResponseEntity.ok(productService.updateAttributeValue(id, request));
    }

    @DeleteMapping("/attribute-values/{id}")
    public ResponseEntity<Void> deleteAttributeValue(@PathVariable Long id) {

        productService.deleteAttributeValue(id);

        return ResponseEntity.noContent().build();
    }
}