package com.dtthuan3.ecommerce.productservice.controller;

import com.dtthuan3.ecommerce.productservice.dto.request.AttributeTemplateItemCreateRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeTemplateItemUpdateRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeTemplateRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeTemplateItemResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeTemplateResponse;
import com.dtthuan3.ecommerce.productservice.service.AttributeTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/attribute-templates")
@RequiredArgsConstructor
public class AttributeTemplateController {

    private final AttributeTemplateService attributeTemplateService;

    // =========================
    // ATTRIBUTE TEMPLATE
    // =========================

    @PostMapping
    public ResponseEntity<AttributeTemplateResponse> create(
            @Valid @RequestBody AttributeTemplateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attributeTemplateService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttributeTemplateResponse> getById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                attributeTemplateService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttributeTemplateResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody AttributeTemplateRequest request
    ) {
        return ResponseEntity.ok(
                attributeTemplateService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID id
    ) {
        attributeTemplateService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    // =========================
    // ATTRIBUTE TEMPLATE ITEM
    // =========================

    @PostMapping("/{templateId}/items")
    public ResponseEntity<AttributeTemplateItemResponse> createItem(
            @PathVariable UUID templateId,
            @Valid @RequestBody AttributeTemplateItemCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attributeTemplateService.createItem(templateId, request));
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<AttributeTemplateItemResponse> getItemById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                attributeTemplateService.getItemById(id)
        );
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<AttributeTemplateItemResponse> updateItem(
            @PathVariable UUID id,
            @Valid @RequestBody AttributeTemplateItemUpdateRequest request
    ) {
        return ResponseEntity.ok(
                attributeTemplateService.updateItem(id, request)
        );
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deactivateItem(
            @PathVariable UUID id
    ) {
        attributeTemplateService.deactivateItem(id);
        return ResponseEntity.noContent().build();
    }
}