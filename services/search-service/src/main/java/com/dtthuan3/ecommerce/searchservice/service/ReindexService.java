package com.dtthuan3.ecommerce.searchservice.service;

import com.dtthuan3.ecommerce.searchservice.client.ProductServiceClient;
import com.dtthuan3.ecommerce.searchservice.dto.ProductDetailDto;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Reindex toan bo san pham PUBLISHED tu product-service (browse paging) roi
 * index lai vao ES. Dung khi: bootstrap lan dau, doi synonyms (synonym ap
 * dung index-time), hoac ES mat du lieu. Idempotent — chay nhieu lan khong hai.
 */
@Service
public class ReindexService {

    private static final Logger log = LoggerFactory.getLogger(ReindexService.class);

    private final ProductServiceClient productService;
    private final ProductIndexService indexService;

    public ReindexService(ProductServiceClient productService, ProductIndexService indexService) {
        this.productService = productService;
        this.indexService = indexService;
    }

    public long reindexAll(int pageSize) {
        int safeSize = Math.min(Math.max(pageSize, 1), 100);
        int page = 0;
        long indexed = 0;
        List<ProductDetailDto> batch;
        do {
            batch = productService.listPublished(page, safeSize);
            for (ProductDetailDto dto : batch) {
                try {
                    indexService.index(dto);
                    indexed++;
                } catch (Exception e) {
                    // 1 product loi khong lam hong ca reindex — log roi di tiep.
                    log.warn("Reindex that bai cho product {}: {}", dto.id(), e.getMessage());
                }
            }
            page++;
        } while (batch.size() == safeSize);
        log.info("Reindex hoan tat: {} documents", indexed);
        return indexed;
    }
}
