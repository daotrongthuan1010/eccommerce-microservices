package com.dtthuan3.ecommerce.productservice.service;

import com.dtthuan3.ecommerce.productservice.dto.request.CreateProductDraftRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductResponse;

import java.util.UUID;

public interface ProductService {

    ProductResponse createDraft(CreateProductDraftRequest request);

    ProductResponse getById(UUID productId);
}