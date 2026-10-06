package com.dtthuan3.ecommerce.productservice.service;

import com.dtthuan3.ecommerce.productservice.dto.request.ProductAttributeValueRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.ProductRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductAttributeValueResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.ProductResponse;

public interface ProductService {

    ProductResponse create(ProductRequest request);

    ProductResponse getById(Long id);

    ProductResponse update(Long id, ProductRequest request);

    void delete(Long id);

    ProductAttributeValueResponse createAttributeValue(
            ProductAttributeValueRequest request
    );

    ProductAttributeValueResponse getAttributeValueById(Long id);

    ProductAttributeValueResponse updateAttributeValue(
            Long id,
            ProductAttributeValueRequest request
    );

    void deleteAttributeValue(Long id);

    ProductResponse submitForApproval(Long id);

    ProductResponse activate(Long id);

    ProductResponse discontinue(Long id);
}