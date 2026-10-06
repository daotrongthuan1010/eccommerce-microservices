package com.dtthuan3.ecommerce.productservice.controller;

import com.dtthuan3.ecommerce.productservice.dto.request.AttributeTemplateItemRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeTemplateRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeTemplateItemResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeTemplateResponse;
import com.dtthuan3.ecommerce.productservice.service.AttributeTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/attribute-templates")
@RequiredArgsConstructor
public class AttributeTemplateController {

    private final AttributeTemplateService attributeTemplateService;

    // =========================
    // ATTRIBUTE TEMPLATE
    // =========================

    @PostMapping
    public ResponseEntity<AttributeTemplateResponse> create(@Valid @RequestBody AttributeTemplateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(attributeTemplateService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttributeTemplateResponse> getById(@PathVariable Long id) {

        return ResponseEntity.ok(attributeTemplateService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttributeTemplateResponse> update(@PathVariable Long id, @Valid @RequestBody AttributeTemplateRequest request) {

        return ResponseEntity.ok(attributeTemplateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        attributeTemplateService.delete(id);

        return ResponseEntity.noContent().build();
    }

    // =========================
    // ATTRIBUTE TEMPLATE ITEM
    // =========================

    @PostMapping("/items")
    public ResponseEntity<AttributeTemplateItemResponse> createItem(@Valid @RequestBody AttributeTemplateItemRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(attributeTemplateService.createItem(request));
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<AttributeTemplateItemResponse> getItemById(@PathVariable Long id) {

        return ResponseEntity.ok(attributeTemplateService.getItemById(id));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<AttributeTemplateItemResponse> updateItem(@PathVariable Long id, @Valid @RequestBody AttributeTemplateItemRequest request) {

        return ResponseEntity.ok(attributeTemplateService.updateItem(id, request));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {

        attributeTemplateService.deleteItem(id);

        return ResponseEntity.noContent().build();
    }
}