package com.dtthuan3.ecommerce.catalogservice.service;

import com.dtthuan3.ecommerce.catalogservice.contstant.BrandsStatus;
import com.dtthuan3.ecommerce.catalogservice.dto.request.BrandsCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.BrandsUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.BrandsResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;

import java.util.List;

public interface BrandsService {

    BrandsResponse create(BrandsCreateRequest request);

    BrandsResponse getById(String id);

    PageResponse<BrandsResponse> getBrands(
            BrandsStatus status,
            String name,
            int page,
            int size
    );

    BrandsResponse update(
            String id,
            BrandsUpdateRequest request
    );

    void delete(String id);

    BrandsResponse updateStatus(
            String id,
            BrandsStatus status
    );

    List<BrandsResponse> getActiveBrands();

    List<BrandsResponse> search(String keyword);
}