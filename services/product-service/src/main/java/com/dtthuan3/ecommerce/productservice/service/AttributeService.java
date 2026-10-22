
package com.dtthuan3.ecommerce.productservice.service;

import com.dtthuan3.ecommerce.productservice.dto.request.AttributeCreateRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeUpdateRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeResponse;

import java.util.UUID;

public interface AttributeService {

    AttributeResponse create(AttributeCreateRequest request);

    AttributeResponse getById(UUID attributeId);

    AttributeResponse update(
            UUID attributeId,
            AttributeUpdateRequest request
    );

    void deactivate(UUID attributeId);
}
