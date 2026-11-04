package com.dtthuan3.ecommerce.catalogservice.service;


import com.dtthuan3.ecommerce.catalogservice.document.ProductSearchDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchProjectionService {

    private final ElasticsearchOperations elasticsearchOperations;

    public void index(ProductSearchDocument document) {

        if (document == null || document.getProductId() == null) {
            throw new IllegalArgumentException(
                    "ProductSearchDocument or productId must not be null"
            );
        }

        ProductSearchDocument saved =
                elasticsearchOperations.save(document);

        log.info(
                "Indexed product into Elasticsearch: productId={}",
                saved.getProductId()
        );
    }
}
