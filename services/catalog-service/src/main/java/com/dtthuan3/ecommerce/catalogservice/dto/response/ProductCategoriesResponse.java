package com.dtthuan3.ecommerce.catalogservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ProductCategoriesResponse {

    private UUID productId;

    private String primaryCategoryId;

    private List<ProductCategoryResponse> categories;
}
