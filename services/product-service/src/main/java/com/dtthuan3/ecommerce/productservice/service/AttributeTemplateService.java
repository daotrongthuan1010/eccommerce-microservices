package com.dtthuan3.ecommerce.productservice.service;

import com.dtthuan3.ecommerce.productservice.dto.request.AttributeTemplateItemCreateRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeTemplateItemUpdateRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeTemplateRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeTemplateItemResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeTemplateResponse;

import java.util.UUID;

public interface AttributeTemplateService {

    AttributeTemplateResponse create(AttributeTemplateRequest request);

    AttributeTemplateResponse getById(UUID templateId);

    AttributeTemplateResponse update(
            UUID templateId,
            AttributeTemplateRequest request
    );

    void deactivate(UUID templateId);

    AttributeTemplateItemResponse createItem(
            UUID templateId,
            AttributeTemplateItemCreateRequest request
    );

    AttributeTemplateItemResponse getItemById(UUID itemId);

    AttributeTemplateItemResponse updateItem(
            UUID itemId,
            AttributeTemplateItemUpdateRequest request
    );

    void deactivateItem(UUID itemId);
}