
package com.dtthuan3.ecommerce.productservice.service;

import com.dtthuan3.ecommerce.productservice.dto.request.AttributeOptionCreateRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeOptionUpdateRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeOptionResponse;

import java.util.List;
import java.util.UUID;

public interface AttributeOptionService {

    AttributeOptionResponse create(
            UUID attributeId,
            AttributeOptionCreateRequest request
    );

    List<AttributeOptionResponse> getActiveOptions(UUID attributeId);

    AttributeOptionResponse update(
            UUID attributeId,
            UUID optionId,
            AttributeOptionUpdateRequest request
    );

    void deactivate(UUID attributeId, UUID optionId);
}
