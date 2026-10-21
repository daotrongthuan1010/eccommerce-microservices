package com.dtthuan3.ecommerce.catalogservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CategoryProductsResponse {

    private String categoryId;

    private List<ProductCategoryResponse> products;
}