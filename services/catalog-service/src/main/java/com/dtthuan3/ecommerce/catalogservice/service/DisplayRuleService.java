package com.dtthuan3.ecommerce.catalogservice.service;

import com.dtthuan3.ecommerce.catalogservice.contstant.DisplayRuleStatus;
import com.dtthuan3.ecommerce.catalogservice.dto.request.DisplayRuleCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.DisplayRuleUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.DisplayRuleResponse;

import java.util.List;

public interface DisplayRuleService {

    DisplayRuleResponse create(
            String collectionId,
            DisplayRuleCreateRequest request
    );

    DisplayRuleResponse getById(
            String id
    );

    List<DisplayRuleResponse> getByCollectionId(
            String collectionId
    );

    List<DisplayRuleResponse> getActiveByCollectionId(
            String collectionId
    );

    DisplayRuleResponse update(
            String id,
            DisplayRuleUpdateRequest request
    );

    DisplayRuleResponse updateStatus(
            String id,
            DisplayRuleStatus status
    );

    void delete(
            String id
    );
}
