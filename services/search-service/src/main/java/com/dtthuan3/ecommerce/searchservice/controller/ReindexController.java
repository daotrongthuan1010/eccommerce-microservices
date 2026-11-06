package com.dtthuan3.ecommerce.searchservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.searchservice.client.ProductServiceClient;
import com.dtthuan3.ecommerce.searchservice.dto.ProductDetailDto;
import com.dtthuan3.ecommerce.searchservice.service.ProductIndexService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin reindex: duyet toan bo san pham PUBLISHED tu product-service
 * (browse paging) roi index lai vao ES. Dung khi: bootstrap lan dau,
 * doi synonyms (synonym ap dung index-time), hoac ES mat du lieu.
 * Chi ADMIN (SecurityConfig). Idempotent — chay nhieu lan khong hai.
 */
@RestController
@RequestMapping("/admin")
public class ReindexController {

    private final ProductServiceClient productService;
    private final ProductIndexService indexService;

    public ReindexController(ProductServiceClient productService, ProductIndexService indexService) {
        this.productService = productService;
        this.indexService = indexService;
    }

    @PostMapping("/reindex")
    public ApiResponse<Map<String, Object>> reindex(
            @RequestParam(defaultValue = "50") int pageSize) {
        int page = 0;
        long indexed = 0;
        List<ProductDetailDto> batch;
        do {
            batch = productService.listPublished(page, pageSize);
            for (ProductDetailDto dto : batch) {
                indexService.index(dto);
                indexed++;
            }
            page++;
        } while (batch.size() == pageSize);

        return ApiResponse.ok("Reindex hoàn tất", Map.of("indexed", indexed));
    }
}
