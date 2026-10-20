package com.dtthuan3.ecommerce.catalogservice.mapper;

import com.dtthuan3.ecommerce.catalogservice.dto.response.ProductCategoryResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.ProductCategory;
import org.springframework.stereotype.Component;

@Component
public class ProductCategoryMapper {

    public ProductCategoryResponse toResponse(
            ProductCategory entity
    ) {
        return ProductCategoryResponse.builder()
                .productId(entity.getProductId())
                .categoryId(entity.getCategoryId())
                .primary(entity.getPrimary())
                .build();
    }
}