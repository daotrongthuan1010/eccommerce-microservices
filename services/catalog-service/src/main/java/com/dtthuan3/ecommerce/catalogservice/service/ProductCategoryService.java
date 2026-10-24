package com.dtthuan3.ecommerce.catalogservice.service;

import com.dtthuan3.ecommerce.catalogservice.dto.request.ProductCategoryUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CategoryProductsResponse;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ProductCategoriesResponse;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ProductCategoryResponse;

import java.util.UUID;

public interface ProductCategoryService {

    ProductCategoriesResponse getProductCategories(
            UUID productId
    );

    ProductCategoriesResponse updateProductCategories(
            UUID productId,
            ProductCategoryUpdateRequest request
    );

    ProductCategoryResponse setPrimaryCategory(
            UUID productId,
            String categoryId
    );

    void removeProductFromCategory(
            UUID productId,
            String categoryId
    );

    CategoryProductsResponse getCategoryProducts(
            String categoryId
    );
}