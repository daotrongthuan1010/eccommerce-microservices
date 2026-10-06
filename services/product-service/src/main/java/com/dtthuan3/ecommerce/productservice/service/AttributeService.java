package com.dtthuan3.ecommerce.productservice.service;

import com.dtthuan3.ecommerce.productservice.dto.request.AttributeOptionRequest;
import com.dtthuan3.ecommerce.productservice.dto.request.AttributeRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeOptionResponse;
import com.dtthuan3.ecommerce.productservice.dto.response.AttributeResponse;

public interface AttributeService {

    AttributeResponse create(AttributeRequest request);

    AttributeResponse getById(Long id);

    AttributeResponse update(Long id, AttributeRequest request);

    void delete(Long id);

    AttributeOptionResponse createOption(
            AttributeOptionRequest request
    );

    AttributeOptionResponse getOptionById(Long id);

    AttributeOptionResponse updateOption(
            Long id,
            AttributeOptionRequest request
    );

    void deleteOption(Long id);
}