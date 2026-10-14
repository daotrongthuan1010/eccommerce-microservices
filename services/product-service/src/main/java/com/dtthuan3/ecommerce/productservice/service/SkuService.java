package com.dtthuan3.ecommerce.productservice.service;

import com.dtthuan3.ecommerce.productservice.dto.request.SkuRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.SkuResponse;

import java.util.Collection;
import java.util.List;

public interface SkuService {

    SkuResponse create(SkuRequest request);

    SkuResponse getById(Long id);

    List<String> findActiveSkuCodes(Collection<String> skuCodes);

    SkuResponse update(Long id, SkuRequest request);

    void delete(Long id);
}
