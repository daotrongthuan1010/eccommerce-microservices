package com.dtthuan3.ecommerce.productservice.service;

import com.dtthuan3.ecommerce.productservice.domain.Sku;
import com.dtthuan3.ecommerce.productservice.dto.request.CreateSkuRequest;

import java.util.UUID;

public interface SkuService {

    Sku create(UUID variantId, CreateSkuRequest request);

    Sku getById(UUID skuId);
}