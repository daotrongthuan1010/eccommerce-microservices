package com.dtthuan3.ecommerce.catalogservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductCategoryResponse {

    private String productId;

    private String categoryId;

    private Boolean primary;
}
