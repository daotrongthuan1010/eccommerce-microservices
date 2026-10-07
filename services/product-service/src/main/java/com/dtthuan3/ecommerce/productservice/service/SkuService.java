package com.dtthuan3.ecommerce.productservice.service;

import com.dtthuan3.ecommerce.productservice.dto.request.SkuRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.SkuResponse;

public interface SkuService {

    SkuResponse create(SkuRequest request);

    SkuResponse getById(Long id);

    SkuResponse update(Long id, SkuRequest request);

    void delete(Long id);
}