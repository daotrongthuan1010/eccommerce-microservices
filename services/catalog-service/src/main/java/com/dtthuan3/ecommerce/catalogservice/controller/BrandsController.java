package com.dtthuan3.ecommerce.catalogservice.controller;

import com.dtthuan3.ecommerce.catalogservice.contstant.BrandsStatus;
import com.dtthuan3.ecommerce.catalogservice.dto.request.BrandsCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.BrandsUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.BrandsResponse;
import com.dtthuan3.ecommerce.catalogservice.service.BrandsService;
import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/brands")
@RequiredArgsConstructor
@Validated
public class BrandsController {

    private final BrandsService brandsService;

    @PostMapping
    public ResponseEntity<ApiResponse<BrandsResponse>> create(
            @Valid @RequestBody BrandsCreateRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.created(
                                brandsService.create(request)
                        )
                );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandsResponse>> getById(
            @PathVariable String id
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        brandsService.getById(id)
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BrandsResponse>>> getBrands(
            @RequestParam(required = false)
            BrandsStatus status,

            @RequestParam(required = false)
            String name,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page không được nhỏ hơn 0")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size phải lớn hơn 0")
            @Max(value = 100, message = "Size không được lớn hơn 100")
            int size
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        brandsService.getBrands(
                                status,
                                name,
                                page,
                                size
                        )
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandsResponse>> update(
            @PathVariable String id,

            @Valid
            @RequestBody BrandsUpdateRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        brandsService.update(id, request)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id
    ) {

        brandsService.delete(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BrandsResponse>> updateStatus(
            @PathVariable String id,

            @RequestParam
            BrandsStatus status
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        brandsService.updateStatus(id, status)
                )
        );
    }

    @GetMapping("/status/active")
    public ResponseEntity<ApiResponse<List<BrandsResponse>>> getActiveBrands() {

        return ResponseEntity.ok(
                ApiResponse.list(
                        brandsService.getActiveBrands()
                )
        );
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<BrandsResponse>>> search(
            @RequestParam String keyword
    ) {

        return ResponseEntity.ok(
                ApiResponse.list(
                        brandsService.search(keyword)
                )
        );
    }
}