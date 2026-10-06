package com.dtthuan3.ecommerce.productservice.service;

import com.dtthuan3.ecommerce.productservice.dto.request.AttributeTemplateItemRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeTemplateRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeTemplateItemResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeTemplateResponse;

public interface AttributeTemplateService {

    AttributeTemplateResponse create(
            AttributeTemplateRequest request
    );

    AttributeTemplateResponse getById(Long id);

    AttributeTemplateResponse update(
            Long id,
            AttributeTemplateRequest request
    );

    void delete(Long id);

    AttributeTemplateItemResponse createItem(
            AttributeTemplateItemRequest request
    );

    AttributeTemplateItemResponse getItemById(Long id);

    AttributeTemplateItemResponse updateItem(
            Long id,
            AttributeTemplateItemRequest request
    );

    void deleteItem(Long id);
}