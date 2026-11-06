package com.dtthuan3.ecommerce.searchservice.service;

import com.dtthuan3.ecommerce.searchservice.dto.ProductDetailDto;
import com.dtthuan3.ecommerce.searchservice.index.ProductDocument;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.stereotype.Service;

/** Ghi document vao ES (index 1 product). Query o SearchService rieng. */
@Service
public class ProductIndexService {

    private final ElasticsearchOperations esOps;

    public ProductIndexService(ElasticsearchOperations esOps) {
        this.esOps = esOps;
    }

    public void index(ProductDetailDto detail) {
        ProductDocument doc = ProductDocument.of(
                detail.id(),
                detail.productCode(),
                detail.displayName(),
                detail.displayShortDescription(),
                detail.priceFrom(),
                detail.categoryId(),
                detail.brandId(),
                null, // primaryImageKey: browse card co; detail chua tra — P2 bo sung
                0,
                detail.status(),
                null);
        // setObject: Spring Data ES tu serialize record → JSON source (khong
        // can build Document map tay — record da la dung shape mapping).
        IndexQuery query = new IndexQuery();
        query.setId(detail.id());
        query.setObject(doc);
        esOps.index(query, IndexCoordinates.of(ProductDocument.INDEX));
    }
}
