package com.dtthuan3.ecommerce.catalogservice.service;


import com.dtthuan3.ecommerce.catalogservice.contstant.SynonymSetStatus;
import com.dtthuan3.ecommerce.catalogservice.dto.request.SynonymSetCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.SynonymSetUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.SynonymSetResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;

public interface SynonymSetService {

    SynonymSetResponse create(
            SynonymSetCreateRequest request
    );

    SynonymSetResponse getById(
            String id
    );

    PageResponse<SynonymSetResponse> getSynonymSets(
            SynonymSetStatus status,
            int page,
            int size
    );

    SynonymSetResponse update(
            String id,
            SynonymSetUpdateRequest request
    );

    SynonymSetResponse updateStatus(
            String id,
            SynonymSetStatus status
    );

    void delete(
            String id
    );
}