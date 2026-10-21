
package com.dtthuan3.ecommerce.productservice.service;

import com.dtthuan3.ecommerce.productservice.dto.request.CreatePriceScheduleRequest;
import com.dtthuan3.ecommerce.productservice.dto.response.PriceScheduleResponse;

import java.util.List;
import java.util.UUID;

public interface PriceScheduleService {

    PriceScheduleResponse create(
            UUID skuId,
            CreatePriceScheduleRequest request
    );

    List<PriceScheduleResponse> getBySkuId(UUID skuId);
}
