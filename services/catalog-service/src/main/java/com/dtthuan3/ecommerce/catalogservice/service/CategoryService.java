package com.dtthuan3.ecommerce.catalogservice.service;

import com.dtthuan3.ecommerce.catalogservice.dto.request.CategoryCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.CategoryUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CategoryResponse;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CategoryTreeResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse create(
            CategoryCreateRequest request
    );

    CategoryResponse getById(
            String id
    );

    List<CategoryResponse> getChildren(
            String parentId
    );

    List<CategoryResponse> getRootCategories();

    List<CategoryTreeResponse> getTree();

    CategoryResponse update(
            String id,
            CategoryUpdateRequest request
    );

    void delete(
            String id
    );
}