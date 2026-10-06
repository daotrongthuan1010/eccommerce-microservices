package com.dtthuan3.ecommerce.catalogservice.service;

import com.dtthuan3.ecommerce.catalogservice.dto.request.BrandsRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.BrandsRespone;
import com.dtthuan3.ecommerce.catalogservice.dto.response.PageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BrandsService {

    BrandsRespone create(
            BrandsRequest request,
            MultipartFile image
    );

    BrandsRespone getByID(Long id);

    List<BrandsRespone> getAll();

    BrandsRespone update(
            Long id,
            BrandsRequest request,
            MultipartFile image
    );

    void delete(Long id);

    PageResponse<BrandsRespone> search(
            String keyword,
            int page,
            int size,
            String sortBy,
            String direction
    );
}