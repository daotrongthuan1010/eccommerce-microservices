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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/product-categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    /**
     * Lấy toàn bộ category của Product.
     */
    @GetMapping("/products/{productId}")
    public ResponseEntity<
            ApiResponse<ProductCategoriesResponse>
            > getProductCategories(
            @PathVariable UUID productId
    ) {

        ProductCategoriesResponse response =
                productCategoryService
                        .getProductCategories(productId);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Lấy category của product thành công",
                        response
                )
        );
    }

    /**
     * Gán / cập nhật toàn bộ category cho Product.
     *
     * ADMIN only.
     */
    @PutMapping("/products/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<
            ApiResponse<ProductCategoriesResponse>
            > updateProductCategories(
            @PathVariable UUID productId,
            @Valid @RequestBody
            ProductCategoryUpdateRequest request
    ) {

        ProductCategoriesResponse response =
                productCategoryService
                        .updateProductCategories(
                                productId,
                                request
                        );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Cập nhật category của product thành công",
                        response
                )
        );
    }

    /**
     * Đặt một category làm primary.
     *
     * ADMIN only.
     */
    @PatchMapping(
            "/products/{productId}/categories/{categoryId}/primary"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<
            ApiResponse<ProductCategoryResponse>
            > setPrimaryCategory(
            @PathVariable UUID productId,
            @PathVariable String categoryId
    ) {

        ProductCategoryResponse response =
                productCategoryService
                        .setPrimaryCategory(
                                productId,
                                categoryId
                        );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Đặt primary category thành công",
                        response
                )
        );
    }

    /**
     * Xóa mapping Product - Category.
     *
     * ADMIN only.
     */
    @DeleteMapping(
            "/products/{productId}/categories/{categoryId}"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<
            ApiResponse<Void>
            > removeProductFromCategory(
            @PathVariable UUID productId,
            @PathVariable String categoryId
    ) {

        productCategoryService
                .removeProductFromCategory(
                        productId,
                        categoryId
                );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Xóa category khỏi product thành công",
                        null
                )
        );
    }

    /**
     * Lấy các Product đang thuộc Category.
     */
    @GetMapping(
            "/categories/{categoryId}/products"
    )
    public ResponseEntity<
            ApiResponse<CategoryProductsResponse>
            > getCategoryProducts(
            @PathVariable String categoryId
    ) {

        CategoryProductsResponse response =
                productCategoryService
                        .getCategoryProducts(categoryId);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Lấy product của category thành công",
                        response
                )
        );
    }
}