package com.dtthuan3.ecommerce.productservice.service;

import com.dtthuan3.ecommerce.productservice.dto.request.ProductVariantRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.VariantAttributeValueRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductVariantResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.VariantAttributeValueResponse;

public interface ProductVariantService {

    ProductVariantResponse create(
            ProductVariantRequest request
    );

    ProductVariantResponse getById(Long id);

    ProductVariantResponse update(
            Long id,
            ProductVariantRequest request
    );

    void delete(Long id);

    VariantAttributeValueResponse createAttributeValue(
            VariantAttributeValueRequest request
    );

    VariantAttributeValueResponse getAttributeValueById(Long id);

    VariantAttributeValueResponse updateAttributeValue(
            Long id,
            VariantAttributeValueRequest request
    );

    void deleteAttributeValue(Long id);
}