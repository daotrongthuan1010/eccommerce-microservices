package com.dtthuan3.ecommerce.catalogservice.repository;

import com.dtthuan3.ecommerce.catalogservice.document.ProductSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSearchRepository
        extends ElasticsearchRepository<ProductSearchDocument, String> {
}
