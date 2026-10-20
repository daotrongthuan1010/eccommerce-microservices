package com.dtthuan3.ecommerce.catalogservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProductCategoriesResponse {

    private String productId;

    private String primaryCategoryId;

    private List<ProductCategoryResponse> categories;
}
