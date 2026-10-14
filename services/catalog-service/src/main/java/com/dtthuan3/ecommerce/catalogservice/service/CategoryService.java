package com.dtthuan3.ecommerce.catalogservice.service;

import com.dtthuan3.ecommerce.catalogservice.dto.request.CategoryRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CategoryResponse;
import org.springframework.web.multipart.MultipartFile;


public interface CategoryService {

    CategoryResponse create(
            CategoryRequest request,
            MultipartFile image
    );

    CategoryResponse getById(
            Long id
    );

    CategoryResponse update(
            Long id,
            CategoryRequest request,
            MultipartFile image
    );

    void delete(
            Long id
    );
}
