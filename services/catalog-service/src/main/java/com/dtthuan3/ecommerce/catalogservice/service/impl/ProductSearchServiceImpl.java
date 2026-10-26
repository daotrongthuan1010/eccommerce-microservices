package com.dtthuan3.ecommerce.catalogservice.service.impl;

import com.dtthuan3.ecommerce.catalogservice.document.ProductSearchDocument;
import com.dtthuan3.ecommerce.catalogservice.dto.search.ProductSearchResult;
import com.dtthuan3.ecommerce.catalogservice.service.ProductSearchService;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public Page<ProductSearchResult> search(
            String keyword,
            Pageable pageable
    ) {

        Query searchQuery = buildSearchQuery(keyword);

        NativeQuery query = NativeQuery.builder()
                .withQuery(searchQuery)
                .withPageable(pageable)
                .build();

        SearchHits<ProductSearchDocument> searchHits =
                elasticsearchOperations.search(
                        query,
                        ProductSearchDocument.class
                );

        List<ProductSearchResult> results =
                searchHits.getSearchHits()
                        .stream()
                        .map(this::toSearchResult)
                        .toList();

        return new PageImpl<>(
                results,
                pageable,
                searchHits.getTotalHits()
        );
    }

    private Query buildSearchQuery(String keyword) {

        return Query.of(q -> q.bool(bool -> bool
                .should(exactSkuQuery(keyword))
                .should(fullTextQuery(keyword))
                .minimumShouldMatch("1")
        ));
    }

    private Query exactSkuQuery(String keyword) {

        return Query.of(q -> q.term(term -> term
                .field("sku")
                .value(keyword)
                .boost(100.0f)
        ));
    }

    private Query fullTextQuery(String keyword) {

        return Query.of(q -> q.multiMatch(m -> m
                .query(keyword)
                .fields(
                        "name^5",
                        "searchText^3",
                        "brand.name^2",
                        "manufacturer.name^2",
                        "categories.name^2",
                        "categories.path"
                )
        ));
    }

    private ProductSearchResult toSearchResult(
            SearchHit<ProductSearchDocument> hit
    ) {

        return ProductSearchResult.builder()
                .product(hit.getContent())
                .score(hit.getScore())
                .build();
    }
}