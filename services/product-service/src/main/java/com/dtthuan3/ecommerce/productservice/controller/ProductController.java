package com.dtthuan3.ecommerce.productservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.productservice.dto.request.CreateProductDraftRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductCardResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductResponse;
import com.dtthuan3.ecommerce.productservice.service.ProductBrowseService;
import com.dtthuan3.ecommerce.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    private final ProductBrowseService productBrowseService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createDraft(
            @Valid @RequestBody CreateProductDraftRequest request
    ) {
        ProductResponse response = productService.createDraft(request);

        return ResponseEntity
                .status(201)
                .body(ApiResponse.created(response));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(
            @PathVariable UUID productId
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Lấy thông tin sản phẩm thành công",
                        productService.getById(productId)
                )
        );
    }

    /**
     * Marketplace browse (2026-11-05 fe-marketplace D4): danh sach san pham da
     * xuat ban cho trang chu / tim kiem / danh muc. GET cong khai (guest) theo
     * SecurityConfig moi; chi doc, khong PII.
     */
    @GetMapping("/browse")
    public ApiResponse<PageResponse<ProductCardResponse>> browse(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        Page<ProductCardResponse> result = productBrowseService.browse(
                page, size, categoryId, brandId, minPrice, maxPrice, keyword, sort
        );
        return ApiResponse.ok(
                "Lấy danh sách sản phẩm thành công",
                PageResponse.of(
                        result.getContent(),
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages()
                )
        );
    }
}