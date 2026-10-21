package com.dtthuan3.ecommerce.catalogservice.service;

import com.dtthuan3.ecommerce.catalogservice.dto.request.SynonymCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.SynonymUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.SynonymResponse;

import java.util.List;

public interface SynonymService {

    SynonymResponse create(
            String synonymSetId,
            SynonymCreateRequest request
    );

    SynonymResponse getById(
            String id
    );

    List<SynonymResponse> getBySynonymSetId(
            String synonymSetId
    );

    SynonymResponse update(
            String id,
            SynonymUpdateRequest request
    );

    void delete(
            String id
    );
}