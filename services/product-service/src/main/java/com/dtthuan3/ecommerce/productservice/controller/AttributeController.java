
package com.dtthuan3.ecommerce.productservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeCreateRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeUpdateRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeResponse;
import com.dtthuan3.ecommerce.productservice.service.AttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attributes")
@RequiredArgsConstructor
public class AttributeController {

    private final AttributeService attributeService;

    @PostMapping
    public ResponseEntity<ApiResponse<AttributeResponse>> create(
            @Valid @RequestBody AttributeCreateRequest request
    ) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(attributeService.create(request)));
    }

    @GetMapping("/{attributeId}")
    public ResponseEntity<ApiResponse<AttributeResponse>> getById(
            @PathVariable UUID attributeId
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Lấy thông tin thuộc tính thành công",
                        attributeService.getById(attributeId)
                )
        );
    }

    @PutMapping("/{attributeId}")
    public ResponseEntity<ApiResponse<AttributeResponse>> update(
            @PathVariable UUID attributeId,
            @Valid @RequestBody AttributeUpdateRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Cập nhật thuộc tính thành công",
                        attributeService.update(attributeId, request)
                )
        );
    }

    @DeleteMapping("/{attributeId}")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable UUID attributeId
    ) {
        attributeService.deactivate(attributeId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
