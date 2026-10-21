package com.dtthuan3.ecommerce.productservice.dto.response;

import com.dtthuan3.ecommerce.productservice.constant.ProductStatus;

import java.util.UUID;

public record ProductCreatedResponse(
        UUID productId,
        UUID revisionId,
        Integer revisionNumber,
        String productCode,
        String name,
        ProductStatus status
) {
}