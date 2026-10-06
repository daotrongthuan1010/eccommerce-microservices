package com.dtthuan3.ecommerce.productservice.controller;

import com.dtthuan3.ecommerce.productservice.dto.request.SkuRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.SkuResponse;
import com.dtthuan3.ecommerce.productservice.service.SkuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/skus")
@RequiredArgsConstructor
public class SkuController {

    private final SkuService skuService;

    @PostMapping
    public ResponseEntity<SkuResponse> create(@Valid @RequestBody SkuRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(skuService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SkuResponse> getById(@PathVariable Long id) {

        return ResponseEntity.ok(skuService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SkuResponse> update(@PathVariable Long id, @Valid @RequestBody SkuRequest request) {

        return ResponseEntity.ok(skuService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        skuService.delete(id);

        return ResponseEntity.noContent().build();
    }
}