package com.dtthuan3.ecommerce.searchservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.searchservice.service.SearchQueryService;
import com.dtthuan3.ecommerce.searchservice.service.SearchQueryService.SearchHitDto;
import com.dtthuan3.ecommerce.searchservice.service.SearchQueryService.SearchResult;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Search API marketplace — GET cong khai (guest tim kiem khong can login).
 * Gateway: /api/search/** → search-service (StripPrefix=2) nen FE goi
 * /api/search/products?q=... → day la /products. Response dung PageResponse
 * chuan cua common (§3) de FE dung chung shape voi browse.
 */
@RestController
@RequestMapping("/products")
public class SearchController {

    private final SearchQueryService searchService;

    public SearchController(SearchQueryService searchService) {
        this.searchService = searchService;
    }

    /** Tim kiem tieng Viet: bo dau + fuzzy + synonym (CocCoc analyzer). */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SearchHitDto>>> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(
                    List.of(), 0, safeSize, 0, 0)));
        }
        SearchResult result = searchService.search(q.trim(), page, safeSize);
        int totalPages = (int) ((result.total() + safeSize - 1) / safeSize);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(
                result.hits(), Math.max(page, 0), safeSize, result.total(), totalPages)));
    }
}
