package com.dtthuan3.ecommerce.productservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.productservice.dto.request.CreateProductDraftRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductResponse;
import com.dtthuan3.ecommerce.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

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
}