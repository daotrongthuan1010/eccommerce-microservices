package com.dtthuan3.ecommerce.catalogservice.controller;

import com.dtthuan3.ecommerce.catalogservice.contstant.DisplayRuleStatus;
import com.dtthuan3.ecommerce.catalogservice.dto.request.DisplayRuleCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.DisplayRuleUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.DisplayRuleResponse;
import com.dtthuan3.ecommerce.catalogservice.service.DisplayRuleService;
import com.dtthuan3.ecommerce.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/display-rules")
@RequiredArgsConstructor
public class DisplayRuleController {

    private final DisplayRuleService displayRuleService;

    /**
     * Tạo rule cho Collection.
     */
    @PostMapping("/collections/{collectionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DisplayRuleResponse>> create(
            @PathVariable String collectionId,
            @Valid @RequestBody DisplayRuleCreateRequest request
    ) {

        DisplayRuleResponse response =
                displayRuleService.create(
                        collectionId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    /**
     * Lấy tất cả rule của Collection.
     */
    @GetMapping("/collections/{collectionId}")
    public ResponseEntity<ApiResponse<List<DisplayRuleResponse>>> getByCollectionId(
            @PathVariable String collectionId
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        displayRuleService
                                .getByCollectionId(collectionId)
                )
        );
    }

    @GetMapping("/abc")
    public ResponseEntity<ApiResponse<List<DisplayRuleResponse>>> getTest(
            @PathVariable String collectionId
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        displayRuleService
                                .getByCollectionId(collectionId)
                )
        );
    }

    /**
     * Lấy các rule ACTIVE của Collection.
     */
    @GetMapping("/collections/{collectionId}/active")
    public ResponseEntity<ApiResponse<List<DisplayRuleResponse>>> getActiveByCollectionId(
            @PathVariable String collectionId
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        displayRuleService
                                .getActiveByCollectionId(collectionId)
                )
        );
    }

    /**
     * Lấy rule theo ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DisplayRuleResponse>> getById(
            @PathVariable String id
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        displayRuleService.getById(id)
                )
        );
    }

    /**
     * Update rule.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DisplayRuleResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody DisplayRuleUpdateRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        displayRuleService.update(
                                id,
                                request
                        )
                )
        );
    }

    /**
     * Update status.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DisplayRuleResponse>> updateStatus(
            @PathVariable String id,
            @RequestParam DisplayRuleStatus status
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        displayRuleService.updateStatus(
                                id,
                                status
                        )
                )
        );
    }

    /**
     * Delete rule.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id
    ) {

        displayRuleService.delete(id);

        return ResponseEntity.ok(
                ApiResponse.ok()
        );
    }
}