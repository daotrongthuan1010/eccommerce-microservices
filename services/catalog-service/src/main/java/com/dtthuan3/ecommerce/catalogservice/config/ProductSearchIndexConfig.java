package com.dtthuan3.ecommerce.catalogservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

@Configuration
@RequiredArgsConstructor
public class ProductSearchIndexConfig {

    private static final String PRODUCT_INDEX_ALIAS = "products";

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * Elasticsearch operations cho Product Search alias.
     *
     * "products" là alias logic.
     *
     * Physical index sẽ được quản lý theo version:
     *
     * products_v1
     * products_v2
     * products_v3
     *
     * Khi reindex xong, alias "products" sẽ được switch
     * sang version mới.
     */
    public IndexOperations productIndexOperations() {
        return elasticsearchOperations.indexOps(
                IndexCoordinates.of(PRODUCT_INDEX_ALIAS)
        );
    }
}