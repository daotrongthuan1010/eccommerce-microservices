package com.dtthuan3.ecommerce.catalogservice.service;

import com.dtthuan3.ecommerce.catalogservice.dto.request.CategoryCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CategoryResponse;
import org.springframework.web.multipart.MultipartFile;


public interface CategoryService {

    CategoryResponse create(
            CategoryCreateRequest request);

    CategoryResponse getById(String id);

    CategoryResponse update(
            Long id,
            CategoryCreateRequest request,
            MultipartFile image
    );

    void delete(
            Long id
    );
}
