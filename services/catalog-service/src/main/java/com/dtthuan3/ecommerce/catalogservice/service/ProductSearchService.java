package com.dtthuan3.ecommerce.catalogservice.service;

import com.dtthuan3.ecommerce.catalogservice.dto.search.ProductSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.dtthuan3.ecommerce.catalogservice.dto.search.CursorSearchResponse;

public interface ProductSearchService {

    Page<ProductSearchResult> search(
            String keyword,
            Pageable pageable
    );

    CursorSearchResponse searchAfter(String keyword, String cursor, int size);
}
