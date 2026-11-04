
package com.dtthuan3.ecommerce.catalogservice.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.dtthuan3.ecommerce.catalogservice.document.ProductSearchDocument;
import com.dtthuan3.ecommerce.catalogservice.dto.search.CursorSearchResponse;
import com.dtthuan3.ecommerce.catalogservice.dto.search.ProductSearchResult;
import com.dtthuan3.ecommerce.catalogservice.service.ProductSearchService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final SearchSynonymDictionary synonymDictionary;



    @Override
    @CircuitBreaker(
            name = "catalogSearch",
            fallbackMethod = "searchFallback"
    )
    @Retry(name = "catalogSearch")
    public Page<ProductSearchResult> search(
            String keyword,
            String categoryId,
            String brandId,
            String status,
            Double minRating,
            Boolean buyable,
            String sort,
            Pageable pageable
    ) {

        Timer.Sample sample = Timer.start(meterRegistry);

        meterRegistry
                .counter(
                        "catalog.search.requests",
                        "mode",
                        "page"
                )
                .increment();

        try {

            String normalizedKeyword = normalize(keyword);

            Query searchQuery = buildSearchQuery(
                    normalizedKeyword,
                    categoryId,
                    brandId,
                    status,
                    minRating,
                    buyable
            );

            NativeQuery query = NativeQuery.builder()
                    .withQuery(searchQuery)
                    .withPageable(pageable)
                    .withSort(sortOptions(sort))
                    .build();

            SearchHits<ProductSearchDocument> searchHits =
                    elasticsearchOperations.search(
                            query,
                            ProductSearchDocument.class
                    );

            List<ProductSearchResult> results =
                    searchHits
                            .getSearchHits()
                            .stream()
                            .map(this::toSearchResult)
                            .toList();

            meterRegistry
                    .counter(
                            "catalog.search.results",
                            "mode",
                            "page"
                    )
                    .increment(results.size());

            return new PageImpl<>(
                    results,
                    pageable,
                    searchHits.getTotalHits()
            );

        } catch (RuntimeException ex) {

            meterRegistry
                    .counter(
                            "catalog.search.errors",
                            "mode",
                            "page"
                    )
                    .increment();

            throw ex;

        } finally {

            sample.stop(
                    meterRegistry.timer(
                            "catalog.search.latency",
                            "mode",
                            "page"
                    )
            );
        }
    }


    @Override
    @CircuitBreaker(
            name = "catalogSearch",
            fallbackMethod = "cursorSearchFallback"
    )
    @Retry(name = "catalogSearch")
    public CursorSearchResponse searchAfter(
            String keyword,
            String categoryId,
            String brandId,
            String status,
            Double minRating,
            Boolean buyable,
            String sort,
            String cursor,
            int size
    ) {

        Timer.Sample sample = Timer.start(meterRegistry);

        meterRegistry
                .counter(
                        "catalog.search.requests",
                        "mode",
                        "cursor"
                )
                .increment();

        String pitId = null;

        try {

            if (size < 1) {
                size = 20;
            }

            if (size > 100) {
                size = 100;
            }

            String normalizedKeyword = normalize(keyword);

            String queryHash = queryHash(
                    normalizedKeyword,
                    categoryId,
                    brandId,
                    status,
                    minRating,
                    buyable,
                    sort
            );

            /*Số lượng sort values phải khớp với số lượng
             * SortOptions hiện tại.
             * Ví dụ:
             * relevance:
             *   _score
             *   sku
             *   _shard_doc
             * => 3 values*/
            int expectedSortValues =
                    cursorSortOptions(sort).size();

            DecodedCursor decoded =
                    decodeCursor(
                            cursor,
                            queryHash,
                            expectedSortValues
                    );

            List<Object> searchAfter =
                    decoded == null
                            ? List.of()
                            : decoded.sortValues();

            pitId =
                    decoded == null
                            ? openPointInTime()
                            : decoded.pitId();

            var queryBuilder =
                    NativeQuery.builder()
                            .withQuery(
                                    buildSearchQuery(
                                            normalizedKeyword,
                                            categoryId,
                                            brandId,
                                            status,
                                            minRating,
                                            buyable
                                    )
                            )
                            .withPageable(
                                    PageRequest.of(
                                            0,
                                            size + 1
                                    )
                            )
                            .withTrackTotalHits(false)
                            .withPointInTime(
                                    new PointInTime(
                                            pitId,
                                            Duration.ofMinutes(2)
                                    )
                            )
                            .withSort(
                                    cursorSortOptions(sort)
                            );

            if (!searchAfter.isEmpty()) {

                queryBuilder.withSearchAfter(
                        searchAfter
                );
            }

            SearchHits<ProductSearchDocument> hits =
                    elasticsearchOperations.search(
                            queryBuilder.build(),
                            ProductSearchDocument.class
                    );

            /* Elasticsearch có thể trả về PIT ID mới.
             * Phải dùng PIT ID mới cho cursor tiếp theo. */
            if (hits.getPointInTimeId() != null) {
                pitId = hits.getPointInTimeId();
            }

            List<SearchHit<ProductSearchDocument>> found =
                    hits.getSearchHits();

            boolean hasMore =
                    found.size() > size;

            List<SearchHit<ProductSearchDocument>> pageHits =
                    hasMore
                            ? found.subList(0, size)
                            : found;

            List<ProductSearchResult> items =
                    pageHits
                            .stream()
                            .map(this::toSearchResult)
                            .toList();

            String nextCursor = null;

            if (hasMore && !pageHits.isEmpty()) {

                SearchHit<ProductSearchDocument> lastHit =
                        pageHits.get(
                                pageHits.size() - 1
                        );

                nextCursor =
                        encodeCursor(
                                lastHit.getSortValues(),
                                queryHash,
                                pitId
                        );

            } else {
                closePointInTime(pitId);
                pitId = null;
            }

            meterRegistry
                    .counter(
                            "catalog.search.results",
                            "mode",
                            "cursor"
                    )
                    .increment(items.size());

            return new CursorSearchResponse(
                    items,
                    nextCursor,
                    hasMore
            );

        } catch (RuntimeException ex) {

            // Nếu request lỗi thì đóng PIT để tránh leak.

            closePointInTime(pitId);

            meterRegistry
                    .counter(
                            "catalog.search.errors",
                            "mode",
                            "cursor"
                    )
                    .increment();

            throw ex;

        } finally {

            sample.stop(
                    meterRegistry.timer(
                            "catalog.search.latency",
                            "mode",
                            "cursor"
                    )
            );
        }
    }



    private Query buildSearchQuery(
            String keyword,
            String categoryId,
            String brandId,
            String status,
            Double minRating,
            Boolean buyable
    ) {

        return Query.of(
                q -> q.bool(
                        bool -> {


                            String normalizedStatus =
                                    status == null || status.isBlank()
                                            ? "ACTIVE"
                                            : status.toUpperCase(
                                            Locale.ROOT
                                    );

                            bool.filter(
                                    statusFilter(
                                            normalizedStatus
                                    )
                            );

                           // CATEGORY
                            if (
                                    categoryId != null
                                            && !categoryId.isBlank()
                            ) {

                                bool.filter(
                                        categoryFilter(categoryId)
                                );
                            }
                            // BRAND

                            if (
                                    brandId != null
                                            && !brandId.isBlank()
                            ) {

                                bool.filter(
                                        brandFilter(brandId)
                                );
                            }

                            // MIN RATING
                            if (minRating != null) {

                                if (
                                        minRating < 0
                                                || minRating > 5
                                ) {

                                    throw new ResponseStatusException(
                                            HttpStatus.BAD_REQUEST,
                                            "minRating phải nằm trong khoảng 0 đến 5"
                                    );
                                }

                                bool.filter(
                                        minRatingFilter(minRating)
                                );
                            }
                            // BUYABLE

                            if (Boolean.TRUE.equals(buyable)) {

                                bool.filter(
                                        buyableFilter()
                                );
                            }

                           // KEYWORD

                            if (keyword == null || keyword.isBlank()) {
                                return bool;
                            }
                            // EXACT SKU

                            bool.should(
                                    exactSkuQuery(
                                            keyword.toUpperCase(
                                                    Locale.ROOT
                                            )
                                    )
                            );

                            // FULL TEXT

                            if (
                                    keyword.codePointCount(
                                            0,
                                            keyword.length()
                                    ) >= 2
                            ) {

                                bool.should(
                                        fullTextQuery(keyword)
                                );

                                synonymDictionary
                                        .expand(keyword)
                                        .forEach(
                                                expansion ->
                                                        bool.should(
                                                                synonymQuery(
                                                                        expansion
                                                                )
                                                        )
                                        );
                                // FUZZY

                                if (
                                        keyword.codePointCount(
                                                0,
                                                keyword.length()
                                        ) >= 4
                                ) {

                                    bool.should(
                                            fuzzyNameQuery(keyword)
                                    );
                                }
                            }

                            return bool.minimumShouldMatch("1");
                        }
                )
        );
    }

    private Query statusFilter(String status) {

        return Query.of(
                q -> q.term(
                        term -> term
                                .field("status")
                                .value(status)
                )
        );
    }

    private Query categoryFilter(String categoryId) {

        return Query.of(
                q -> q.term(
                        term -> term
                                .field("categories.id")
                                .value(categoryId)
                )
        );
    }

    private Query brandFilter(String brandId) {

        return Query.of(
                q -> q.term(
                        term -> term
                                .field("brand.id")
                                .value(brandId)
                )
        );
    }

    private Query minRatingFilter(Double minRating) {

        return Query.of(
                q -> q.range(
                        range -> range.number(
                                number -> number
                                        .field("rating.avg")
                                        .gte(minRating)
                        )
                )
        );
    }

    private Query buyableFilter() {

        return Query.of(
                q -> q.range(
                        range -> range.number(
                                number -> number
                                        .field("stock.available")
                                        .gt(0.0)
                        )
                ));
    }

    private Query exactSkuQuery(String keyword) {

        return Query.of(
                q -> q.term(
                        term -> term
                                .field("sku")
                                .value(keyword)
                                .boost(100.0f)
                )
        );
    }

    private Query fullTextQuery(String keyword) {

        return Query.of(
                q -> q.multiMatch(
                        m -> m
                                .query(keyword)
                                .fields(
                                        "name^5",
                                        "searchText^3",
                                        "brand.name^2",
                                        "manufacturer.name^2",
                                        "categories.name^2",
                                        "categories.path"
                                )
                )
        );
    }

    private Query fuzzyNameQuery(String keyword) {

        return Query.of(
                q -> q.match(
                        match -> match
                                .field("name")
                                .query(keyword)
                                .fuzziness("AUTO")
                                .prefixLength(2)
                                .maxExpansions(25)
                                .boost(0.35f)
                )
        );
    }

    private Query synonymQuery(String expandedKeyword) {

        return Query.of(
                q -> q.multiMatch(
                        m -> m
                                .query(expandedKeyword)
                                .fields(
                                        "name^5",
                                        "searchText^3",
                                        "brand.name^2",
                                        "manufacturer.name^2",
                                        "categories.name^2",
                                        "categories.path"
                                )
                                .boost(0.7f)
                )
        );
    }

    private List<SortOptions> sortOptions(String sort) {

        // nếu sort bằng null hoặc có khoảng trống không có giá trị
        // thì sort = relevance
        // ngược lại chuyển sort thành chữ thường
        String normalizedSort =
                sort == null || sort.isBlank()
                        ? "relevance"
                        : sort.toLowerCase(Locale.ROOT);

        return switch (normalizedSort) {

            case "relevance" -> List.of(
                    // sắp xếp theo score theo thứ tự giảm dần
                    SortOptions.of(
                            s -> s.score(
                                    score -> score.order(
                                            SortOrder.Desc
                                    )
                            )
                    ),

                    // nếu score bằng nhau thì sắp xep theo sku tăng dần
                    SortOptions.of(
                            s -> s.field(
                                    field -> field
                                            .field("sku")
                                            .order(SortOrder.Asc)
                            )
                    )
            );

            case "newest" -> List.of(

                    SortOptions.of(
                            s -> s.field(
                                    field -> field
                                            .field("updatedAt")
                                            .order(SortOrder.Desc)
                            )
                    ),

                    SortOptions.of(
                            s -> s.field(
                                    field -> field
                                            .field("sku")
                                            .order(SortOrder.Asc)
                            )
                    )
            );

            case "rating" -> List.of(

                    SortOptions.of(
                            s -> s.field(
                                    field -> field
                                            .field("rating.avg")
                                            .order(SortOrder.Desc)
                            )
                    ),

                    SortOptions.of(
                            s -> s.field(
                                    field -> field
                                            .field("rating.count")
                                            .order(SortOrder.Desc)
                            )
                    ),

                    SortOptions.of(
                            s -> s.field(
                                    field -> field
                                            .field("sku")
                                            .order(SortOrder.Asc)
                            )
                    )
            );

            case "price", "best-selling" ->

                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "sort=" + normalizedSort
                                    + " chưa khả dụng vì SearchDocument hiện chưa có field tương ứng"
                    );

            default ->

                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "sort không hợp lệ. Cho phép: relevance, newest, rating"
                    );
        };
    }

    private List<SortOptions> cursorSortOptions(String sort) {

        List<SortOptions> base =
                sortOptions(sort);

        /*Thêm _shard_doc làm tie-breaker.
         * relevance:
         *   _score DESC
         *   sku ASC
         *   _shard_doc ASC
         * rating:
         *   rating.avg DESC
         *   rating.count DESC
         *   sku ASC
         *   _shard_doc ASC
         */
        return java.util.stream.Stream.concat(
                base.stream(),
                java.util.stream.Stream.of(
                        SortOptions.of(
                                s -> s.field(
                                        field -> field
                                                .field("_shard_doc")
                                                .order(SortOrder.Asc)
                                )
                        )
                )
        ).toList();
    }

    private DecodedCursor decodeCursor(
            String cursor,
            String queryHash,
            int expectedSortValues
    ) {

        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {

            byte[] json =
                    Base64.getUrlDecoder()
                            .decode(cursor);

            Map<String, Object> payload =
                    objectMapper.readValue(
                            json,
                            new TypeReference<>() {}
                    );

            // ---------------------------------------------
            // QUERY HASH
            // ---------------------------------------------

            if (
                    !queryHash.equals(
                            payload.get("query")
                    )
            ) {

                throw new IllegalArgumentException(
                        "Cursor không thuộc query hiện tại"
                );
            }

            // ---------------------------------------------
            // SORT VALUES
            // ---------------------------------------------

            Object sortObject =
                    payload.get("sort");

            if (!(sortObject instanceof List<?> values)) {

                throw new IllegalArgumentException(
                        "Cursor thiếu sort values"
                );
            }

            /*Ví dụ cursor được tạo lúc sort có:
             *   _score
             *   sku
             *   _shard_doc
             * thì cursor bắt buộc phải có đúng 3 values.
             */
            if (values.size() != expectedSortValues) {

                throw new IllegalArgumentException(
                        "Cursor sort values không khớp với sort hiện tại"
                );
            }

            Object pitObject =
                    payload.get("pit");

            if (
                    !(pitObject instanceof String pit)
                            || pit.isBlank()
            ) {

                throw new IllegalArgumentException(
                        "Cursor PIT không hợp lệ"
                );
            }

            return new DecodedCursor(
                    values.stream()
                            .map(value -> (Object) value)
                            .toList(),
                    pit
            );

        } catch (ResponseStatusException ex) {

            throw ex;

        } catch (Exception ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cursor không hợp lệ",
                    ex
            );
        }
    }

    private String encodeCursor(
            List<Object> sortValues,
            String queryHash,
            String pitId
    ) {

        try {

            return Base64
                    .getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(
                            objectMapper.writeValueAsBytes(
                                    Map.of(
                                            "query",
                                            queryHash,
                                            "sort",
                                            sortValues,
                                            "pit",
                                            pitId
                                    )
                            )
                    );

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Không thể tạo cursor tìm kiếm",
                    ex
            );
        }
    }

    private String openPointInTime() {

        try {

            return elasticsearchClient
                    .openPointInTime(
                            open -> open
                                    .index("products")
                                    .keepAlive(
                                            keepAlive ->
                                                    keepAlive.time("2m")
                                    )
                    )
                    .id();

        } catch (Exception ex) {

            throw new DataAccessResourceFailureException(
                    "Không thể mở Elasticsearch PIT cho catalog search",
                    ex
            );
        }
    }

    private void closePointInTime(String pitId) {

        if (
                pitId == null
                        || pitId.isBlank()
        ) {
            return;
        }

        try {

            elasticsearchClient.closePointInTime(
                    close -> close.id(pitId)
            );

        } catch (Exception ignored) {
            // PIT tự expire sau keep-alive.
        }
    }

    private String queryHash(
            String keyword,
            String categoryId,
            String brandId,
            String status,
            Double minRating,
            Boolean buyable,
            String sort
    ) {

        String raw =
                String.join(
                        "|",
                        safe(keyword),
                        safe(categoryId),
                        safe(brandId),
                        safe(status),
                        String.valueOf(minRating),
                        String.valueOf(buyable),
                        safe(sort)
                );

        try {

            byte[] digest =
                    MessageDigest
                            .getInstance("SHA-256")
                            .digest(
                                    raw.getBytes(
                                            StandardCharsets.UTF_8
                                    )
                            );

            return Base64
                    .getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(digest);

        } catch (NoSuchAlgorithmException ex) {

            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    ex
            );
        }
    }

    private String safe(String value) {

        return value == null ? "" : value;
    }

    private ProductSearchResult toSearchResult(
            SearchHit<ProductSearchDocument> hit
    ) {

        return ProductSearchResult
                .builder()
                .product(hit.getContent())
                .score(hit.getScore())
                .build();
    }

    private String normalize(String keyword) {

        if (keyword == null) {
            return "";
        }

        return keyword
                .strip()
                .replaceAll("\\s+", " ");
    }

    private record DecodedCursor(
            List<Object> sortValues,
            String pitId
    ) {
    }
}
