package com.dtthuan3.ecommerce.catalogservice.service;

import com.dtthuan3.ecommerce.catalogservice.dto.search.CursorSearchResponse;
import com.dtthuan3.ecommerce.catalogservice.dto.search.ProductSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductSearchService {

    Page<ProductSearchResult> search(
            String keyword,
            String categoryId,
            String brandId,
            String status,
            Double minRating,
            Boolean buyable,
            String sort,
            Pageable pageable
    );

    CursorSearchResponse searchAfter(
            String keyword,
            String categoryId,
            String brandId,
            String status,
            Double minRating,
            Boolean buyable,
            String sort,
            String cursor,
            int size
    );
}