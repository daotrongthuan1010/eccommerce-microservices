package com.dtthuan3.ecommerce.productservice.controller;

import com.dtthuan3.ecommerce.productservice.dto.request.AttributeOptionRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeOptionResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeResponse;
import com.dtthuan3.ecommerce.productservice.service.AttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/attributes")
@RequiredArgsConstructor
public class AttributeController {

    private final AttributeService attributeService;

    // =========================
    // ATTRIBUTE
    // =========================

    @PostMapping
    public ResponseEntity<AttributeResponse> create(@Valid @RequestBody AttributeRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(attributeService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttributeResponse> getById(@PathVariable Long id) {

        return ResponseEntity.ok(attributeService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttributeResponse> update(@PathVariable Long id, @Valid @RequestBody AttributeRequest request) {

        return ResponseEntity.ok(attributeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        attributeService.delete(id);

        return ResponseEntity.noContent().build();
    }

    // =========================
    // ATTRIBUTE OPTION
    // =========================

    @PostMapping("/options")
    public ResponseEntity<AttributeOptionResponse> createOption(@Valid @RequestBody AttributeOptionRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(attributeService.createOption(request));
    }

    @GetMapping("/options/{id}")
    public ResponseEntity<AttributeOptionResponse> getOptionById(@PathVariable Long id) {

        return ResponseEntity.ok(attributeService.getOptionById(id));
    }

    @PutMapping("/options/{id}")
    public ResponseEntity<AttributeOptionResponse> updateOption(@PathVariable Long id, @Valid @RequestBody AttributeOptionRequest request) {

        return ResponseEntity.ok(attributeService.updateOption(id, request));
    }

    @DeleteMapping("/options/{id}")
    public ResponseEntity<Void> deleteOption(@PathVariable Long id) {

        attributeService.deleteOption(id);

        return ResponseEntity.noContent().build();
    }
}