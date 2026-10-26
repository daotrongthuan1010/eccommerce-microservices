package com.dtthuan3.ecommerce.catalogservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProductCategoryResponse {

    private UUID productId;

    private String categoryId;

    private Boolean primary;
}