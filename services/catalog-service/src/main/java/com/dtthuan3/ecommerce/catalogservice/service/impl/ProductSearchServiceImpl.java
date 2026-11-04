package com.dtthuan3.ecommerce.catalogservice.service.impl;

import com.dtthuan3.ecommerce.catalogservice.document.ProductSearchDocument;
import com.dtthuan3.ecommerce.catalogservice.dto.search.ProductSearchResult;
import com.dtthuan3.ecommerce.catalogservice.dto.search.CursorSearchResponse;
import com.dtthuan3.ecommerce.catalogservice.service.ProductSearchService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query.PointInTime;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final SearchSynonymDictionary synonymDictionary;

    @Override
    @CircuitBreaker(name = "catalogSearch", fallbackMethod = "searchFallback")
    @Retry(name = "catalogSearch")
    public Page<ProductSearchResult> search(
            String keyword,
            Pageable pageable
    ) {

        Timer.Sample sample = Timer.start(meterRegistry);
        meterRegistry.counter("catalog.search.requests", "mode", "page").increment();
        try {
            String normalizedKeyword = normalize(keyword);
            Query searchQuery = buildSearchQuery(normalizedKeyword);
            NativeQuery query = NativeQuery.builder().withQuery(searchQuery)
                    .withPageable(pageable).withSort(sortOptions()).build();
            SearchHits<ProductSearchDocument> searchHits = elasticsearchOperations.search(
                    query, ProductSearchDocument.class);
            List<ProductSearchResult> results = searchHits.getSearchHits().stream()
                    .map(this::toSearchResult).toList();
            meterRegistry.counter("catalog.search.results", "mode", "page").increment(results.size());
            return new PageImpl<>(results, pageable, searchHits.getTotalHits());
        } catch (RuntimeException ex) {
            meterRegistry.counter("catalog.search.errors", "mode", "page").increment();
            throw ex;
        } finally {
            sample.stop(meterRegistry.timer("catalog.search.latency", "mode", "page"));
        }
    }

    @Override
    @CircuitBreaker(name = "catalogSearch", fallbackMethod = "cursorSearchFallback")
    @Retry(name = "catalogSearch")
    public CursorSearchResponse searchAfter(String keyword, String cursor, int size) {
        Timer.Sample sample = Timer.start(meterRegistry);
        meterRegistry.counter("catalog.search.requests", "mode", "cursor").increment();
        String pitId = null;
        try {
            String normalizedKeyword = normalize(keyword);
            String queryHash = queryHash(normalizedKeyword);
            DecodedCursor decoded = decodeCursor(cursor, queryHash);
            List<Object> searchAfter = decoded == null ? List.of() : decoded.sortValues();
            pitId = decoded == null ? openPointInTime() : decoded.pitId();

            var queryBuilder = NativeQuery.builder()
                    .withQuery(buildSearchQuery(normalizedKeyword))
                    .withPageable(PageRequest.of(0, size + 1))
                    .withTrackTotalHits(false)
                    .withPointInTime(new PointInTime(pitId, Duration.ofMinutes(2)))
                    .withSort(cursorSortOptions());
            if (!searchAfter.isEmpty()) queryBuilder.withSearchAfter(searchAfter);

            SearchHits<ProductSearchDocument> hits = elasticsearchOperations.search(
                    queryBuilder.build(), ProductSearchDocument.class);
            if (hits.getPointInTimeId() != null) pitId = hits.getPointInTimeId();
            List<SearchHit<ProductSearchDocument>> found = hits.getSearchHits();
            boolean hasMore = found.size() > size;
            List<SearchHit<ProductSearchDocument>> pageHits = hasMore ? found.subList(0, size) : found;
            List<ProductSearchResult> items = pageHits.stream().map(this::toSearchResult).toList();
            String nextCursor = hasMore && !pageHits.isEmpty()
                    ? encodeCursor(pageHits.get(pageHits.size() - 1).getSortValues(), queryHash, pitId)
                    : null;
            if (!hasMore) closePointInTime(pitId);
            meterRegistry.counter("catalog.search.results", "mode", "cursor").increment(items.size());
            return new CursorSearchResponse(items, nextCursor, hasMore);
        } catch (RuntimeException ex) {
            closePointInTime(pitId);
            meterRegistry.counter("catalog.search.errors", "mode", "cursor").increment();
            throw ex;
        } finally {
            sample.stop(meterRegistry.timer("catalog.search.latency", "mode", "cursor"));
        }
    }

    private Page<ProductSearchResult> searchFallback(String keyword, Pageable pageable, Throwable cause) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Tìm kiếm Catalog hiện tạm thời không khả dụng", cause);
    }

    private CursorSearchResponse cursorSearchFallback(String keyword, String cursor, int size, Throwable cause) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Tìm kiếm Catalog hiện tạm thời không khả dụng", cause);
    }

    private Query buildSearchQuery(String keyword) {

        return Query.of(q -> q.bool(bool -> {
            bool.filter(activeProductsOnly());
            bool.should(exactSkuQuery(keyword.toUpperCase(Locale.ROOT)));
            if (keyword.codePointCount(0, keyword.length()) >= 2) {
                bool.should(namePrefixQuery(keyword));
                bool.should(fullTextQuery(keyword));
                synonymDictionary.expand(keyword).forEach(expansion -> bool.should(synonymQuery(expansion)));
                if (keyword.codePointCount(0, keyword.length()) >= 4) {
                    bool.should(fuzzyNameQuery(keyword));
                }
            }
            return bool.minimumShouldMatch("1");
        }));
    }

    private Query activeProductsOnly() {
        return Query.of(q -> q.term(term -> term
                .field("status")
                .value("ACTIVE")
        ));
    }

    private Query exactSkuQuery(String keyword) {

        return Query.of(q -> q.term(term -> term
                .field("sku")
                .value(keyword)
                .boost(100.0f)
        ));
    }

    private Query namePrefixQuery(String keyword) {
        return Query.of(q -> q.multiMatch(prefix -> prefix
                .type(TextQueryType.BoolPrefix)
                .fields("name.autocomplete", "name.autocomplete._2gram", "name.autocomplete._3gram")
                .query(keyword)
                .boost(12.0f)
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

    private Query fuzzyNameQuery(String keyword) {
        return Query.of(q -> q.match(match -> match
                .field("name")
                .query(keyword)
                .fuzziness("AUTO")
                .prefixLength(2)
                .maxExpansions(25)
                .boost(0.35f)
        ));
    }

    private Query synonymQuery(String expandedKeyword) {
        return Query.of(q -> q.multiMatch(m -> m
                .query(expandedKeyword)
                .fields("name^5", "searchText^3", "brand.name^2", "manufacturer.name^2",
                        "categories.name^2", "categories.path")
                .boost(0.7f)
        ));
    }

    private List<SortOptions> sortOptions() {
        return List.of(
                SortOptions.of(sort -> sort.score(score -> score
                        .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))),
                SortOptions.of(sort -> sort.field(field -> field.field("sku")
                        .order(co.elastic.clients.elasticsearch._types.SortOrder.Asc)))
        );
    }

    private List<SortOptions> cursorSortOptions() {
        return List.of(
                sortOptions().get(0),
                sortOptions().get(1),
                SortOptions.of(sort -> sort.field(field -> field.field("_shard_doc")
                        .order(co.elastic.clients.elasticsearch._types.SortOrder.Asc)))
        );
    }

    private DecodedCursor decodeCursor(String cursor, String queryHash) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            byte[] json = Base64.getUrlDecoder().decode(cursor);
            Map<String, Object> payload = objectMapper.readValue(json, new TypeReference<>() {});
            if (!queryHash.equals(payload.get("query")) || !(payload.get("sort") instanceof List<?> values)
                    || values.size() != 3 || !(values.get(0) instanceof Number)
                    || !(values.get(1) instanceof String) || !(values.get(2) instanceof Number)
                    || !(payload.get("pit") instanceof String pit)
                    || pit.isBlank()) {
                throw new IllegalArgumentException("Cursor không hợp lệ hoặc không thuộc từ khóa này");
            }
            return new DecodedCursor(List.of(values.get(0), values.get(1)), pit);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor không hợp lệ", ex);
        }
    }

    private String encodeCursor(List<Object> sortValues, String queryHash, String pitId) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    objectMapper.writeValueAsBytes(Map.of("query", queryHash, "sort", sortValues, "pit", pitId)));
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tạo cursor tìm kiếm", ex);
        }
    }

    private String openPointInTime() {
        try {
            return elasticsearchClient.openPointInTime(open -> open.index("products")
                    .keepAlive(keepAlive -> keepAlive.time("2m"))).id();
        } catch (Exception ex) {
            throw new DataAccessResourceFailureException("Không thể mở Elasticsearch PIT cho catalog search", ex);
        }
    }

    private void closePointInTime(String pitId) {
        if (pitId == null || pitId.isBlank()) return;
        try {
            elasticsearchClient.closePointInTime(close -> close.id(pitId));
        } catch (Exception ignored) {
            // PIT also expires automatically after its keep-alive interval.
        }
    }

    private record DecodedCursor(List<Object> sortValues, String pitId) { }

    private String queryHash(String keyword) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(keyword.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private ProductSearchResult toSearchResult(
            SearchHit<ProductSearchDocument> hit
    ) {

        return ProductSearchResult.builder()
                .product(hit.getContent())
                .score(hit.getScore())
                .build();
    }

    private String normalize(String keyword) {
        String normalized = keyword == null ? "" : keyword.strip().replaceAll("\\s+", " ");
        return normalized;
    }
}
