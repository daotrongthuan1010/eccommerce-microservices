package com.dtthuan3.ecommerce.productservice.controller;

import com.dtthuan3.ecommerce.productservice.dto.request.SkuRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.SkuValidationRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.SkuResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.SkuValidationResponse;
import com.dtthuan3.ecommerce.productservice.service.SkuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/skus")
@RequiredArgsConstructor
public class SkuController {

    private final SkuService skuService;

    @PostMapping("/validate")
    public ResponseEntity<SkuValidationResponse> validate(@Valid @RequestBody SkuValidationRequest request) {
        Set<String> requested = new HashSet<>(request.skuCodes());
        List<String> active = skuService.findActiveSkuCodes(requested);
        Set<String> activeSet = new HashSet<>(active);
        List<String> invalid = requested.stream().filter(code -> !activeSet.contains(code)).sorted().toList();
        return ResponseEntity.ok(new SkuValidationResponse(active.stream().sorted().toList(), invalid));
    }

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
