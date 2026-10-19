
package com.dtthuan3.ecommerce.productservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeOptionCreateRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeOptionUpdateRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeOptionResponse;
import com.dtthuan3.ecommerce.productservice.service.AttributeOptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attributes/{attributeId}/options")
@RequiredArgsConstructor
public class AttributeOptionController {

    private final AttributeOptionService attributeOptionService;

    @PostMapping
    public ResponseEntity<ApiResponse<AttributeOptionResponse>> create(
            @PathVariable UUID attributeId,
            @Valid @RequestBody AttributeOptionCreateRequest request
    ) {
        return ResponseEntity.status(201).body(
                ApiResponse.created(
                        attributeOptionService.create(attributeId, request)
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AttributeOptionResponse>>> getActiveOptions(
            @PathVariable UUID attributeId
    ) {
        return ResponseEntity.ok(
                ApiResponse.list(
                        attributeOptionService.getActiveOptions(attributeId)
                )
        );
    }

    @PutMapping("/{optionId}")
    public ResponseEntity<ApiResponse<AttributeOptionResponse>> update(
            @PathVariable UUID attributeId,
            @PathVariable UUID optionId,
            @Valid @RequestBody AttributeOptionUpdateRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Cập nhật option thành công",
                        attributeOptionService.update(
                                attributeId,
                                optionId,
                                request
                        )
                )
        );
    }

    @DeleteMapping("/{optionId}")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable UUID attributeId,
            @PathVariable UUID optionId
    ) {
        attributeOptionService.deactivate(attributeId, optionId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
