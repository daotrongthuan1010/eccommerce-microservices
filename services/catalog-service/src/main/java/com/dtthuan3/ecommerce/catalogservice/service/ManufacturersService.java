package com.dtthuan3.ecommerce.catalogservice.service;

import com.dtthuan3.ecommerce.catalogservice.contstant.ManufacturerStatus;
import com.dtthuan3.ecommerce.catalogservice.dto.request.ManufacturerCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.ManufacturerUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ManufacturerResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;

import java.util.List;

public interface ManufacturersService {

    ManufacturerResponse create(
            ManufacturerCreateRequest request
    );

    ManufacturerResponse getById(
            String id
    );

    PageResponse<ManufacturerResponse> getManufacturers(
            ManufacturerStatus status,
            String name,
            int page,
            int size
    );

    ManufacturerResponse update(
            String id,
            ManufacturerUpdateRequest request
    );

    void delete(
            String id
    );

    ManufacturerResponse updateStatus(
            String id,
            ManufacturerStatus status
    );

    List<ManufacturerResponse> getActiveManufacturers();

    List<ManufacturerResponse> search(
            String keyword
    );
}