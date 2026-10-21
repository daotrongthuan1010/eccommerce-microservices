package com.dtthuan3.ecommerce.catalogservice.service;



import com.dtthuan3.ecommerce.catalogservice.contstant.CollectionStatus;
import com.dtthuan3.ecommerce.catalogservice.dto.request.CollectionCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.CollectionReorderRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.CollectionUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CollectionResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;


import java.util.List;

public interface CollectionService {

    CollectionResponse create(
            CollectionCreateRequest request
    );

    CollectionResponse getById(
            String id
    );

    PageResponse<CollectionResponse> getCollections(
            CollectionStatus status,
            String keyword,
            int page,
            int size
    );

    CollectionResponse update(
            String id,
            CollectionUpdateRequest request
    );

    CollectionResponse updateStatus(
            String id,
            CollectionStatus status
    );

    void delete(
            String id
    );

    List<CollectionResponse> getActiveCollections();

    List<CollectionResponse> getHomepageCollections();

    CollectionResponse publish(
            String id
    );

    CollectionResponse unpublish(
            String id
    );

    void reorder(
            CollectionReorderRequest request
    );
}