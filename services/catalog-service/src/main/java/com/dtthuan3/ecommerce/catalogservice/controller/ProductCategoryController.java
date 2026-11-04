package com.dtthuan3.ecommerce.catalogservice.controller;

import com.dtthuan3.ecommerce.catalogservice.dto.request.ProductCategoryUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CategoryProductsResponse;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ProductCategoriesResponse;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ProductCategoryResponse;
import com.dtthuan3.ecommerce.catalogservice.service.ProductCategoryService;
import com.dtthuan3.ecommerce.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/product-categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;


    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<ProductCategoriesResponse>>
    getProductCategories(
            @PathVariable UUID productId
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Lấy danh sách category của product thành công",
                        productCategoryService
                                .getProductCategories(productId)
                )
        );
    }


    @PutMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<ProductCategoriesResponse>>
    updateProductCategories(
            @PathVariable UUID productId,

            @Valid
            @RequestBody
            ProductCategoryUpdateRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Cập nhật category của product thành công",
                        productCategoryService
                                .updateProductCategories(
                                        productId,
                                        request
                                )
                )
        );
    }


    @PatchMapping(
            "/products/{productId}/categories/{categoryId}/primary"
    )
    public ResponseEntity<ApiResponse<ProductCategoryResponse>>
    setPrimaryCategory(
            @PathVariable UUID productId,
            @PathVariable String categoryId
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Đặt primary category thành công",
                        productCategoryService
                                .setPrimaryCategory(
                                        productId,
                                        categoryId
                                )
                )
        );
    }


    @DeleteMapping(
            "/products/{productId}/categories/{categoryId}"
    )
    public ResponseEntity<ApiResponse<Void>>
    removeProductFromCategory(
            @PathVariable UUID productId,
            @PathVariable String categoryId
    ) {

        productCategoryService.removeProductFromCategory(
                productId,
                categoryId
        );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Xóa product khỏi category thành công",
                        null
                )
        );
    }


    @GetMapping("/categories/{categoryId}/products")
    public ResponseEntity<ApiResponse<CategoryProductsResponse>>
    getCategoryProducts(
            @PathVariable String categoryId
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Lấy danh sách product của category thành công",
                        productCategoryService
                                .getCategoryProducts(categoryId)
                )
        );
    }
}