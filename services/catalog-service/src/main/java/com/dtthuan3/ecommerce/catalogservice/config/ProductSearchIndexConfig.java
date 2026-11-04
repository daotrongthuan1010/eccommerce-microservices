package com.dtthuan3.ecommerce.catalogservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import com.dtthuan3.ecommerce.catalogservice.document.ProductSearchDocument;

@Configuration
@RequiredArgsConstructor
public class ProductSearchIndexConfig {

    private static final String PRODUCT_INDEX_ALIAS = "products";

    private final ElasticsearchOperations elasticsearchOperations;

    /** Operations target the logical "products" name; alias lifecycle is managed by deployment. */
    @Bean
    public IndexOperations productIndexOperations() {
        return elasticsearchOperations.indexOps(
                IndexCoordinates.of(PRODUCT_INDEX_ALIAS)
        );
    }

    /**
     * Optional bootstrap for an empty environment. Existing production indexes are never
     * rewritten here; mapping changes require a versioned reindex and alias cutover.
     */
    @Bean
    @ConditionalOnProperty(name = "catalog.search.index.bootstrap-enabled", havingValue = "true")
    public ApplicationRunner bootstrapProductSearchIndex() {
        return args -> {
            IndexOperations index = elasticsearchOperations.indexOps(ProductSearchDocument.class);
            if (!index.exists()) {
                index.createWithMapping();
            }
        };
    }
}
