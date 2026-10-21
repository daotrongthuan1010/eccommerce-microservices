package com.dtthuan3.ecommerce.productservice.service;

import com.dtthuan3.ecommerce.productservice.domain.ProductVariant;
import com.dtthuan3.ecommerce.productservice.dto.request.CreateProductVariantRequest;

import java.util.List;
import java.util.UUID;

public interface ProductVariantService {

    ProductVariant create(UUID productId, CreateProductVariantRequest request);

    ProductVariant getById(UUID variantId);

    List<ProductVariant> getByProductId(UUID productId);
}