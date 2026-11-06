package com.dtthuan3.ecommerce.searchservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import com.dtthuan3.ecommerce.searchservice.service.SearchQueryService;
import com.dtthuan3.ecommerce.searchservice.service.SearchQueryService.SearchHitDto;
import com.dtthuan3.ecommerce.searchservice.service.SearchQueryService.SearchResult;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Search API marketplace — GET cong khai (guest tim kiem khong can login),
 * reindex chi ADMIN. Gateway: /api/search/** → search-service (StripPrefix=2)
 * nen FE goi /api/search/products?q=... → day la /products.
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "content", List.of(),
                    "page", 0, "size", size,
                    "totalElements", 0, "totalPages", 0)));
        }
        SearchResult result = searchService.search(q.trim(), page, size);
        List<SearchHitDto> content = result.hits();
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "content", content,
                "page", page,
                "size", size,
                "totalElements", result.total(),
                "totalPages", (result.total() + size - 1) / Math.max(size, 1))));
    }
}
