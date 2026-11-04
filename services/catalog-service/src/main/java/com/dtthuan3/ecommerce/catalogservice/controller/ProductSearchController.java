package com.dtthuan3.ecommerce.catalogservice.controller;

import com.dtthuan3.ecommerce.catalogservice.dto.search.CursorSearchResponse;
import com.dtthuan3.ecommerce.catalogservice.dto.search.ProductSearchReplayStatus;
import com.dtthuan3.ecommerce.catalogservice.dto.search.ProductSearchResult;
import com.dtthuan3.ecommerce.catalogservice.dto.search.SearchDltReplayResponse;
import com.dtthuan3.ecommerce.catalogservice.service.ProductSearchService;
import com.dtthuan3.ecommerce.catalogservice.service.impl.ProductSearchDltReplayService;
import com.dtthuan3.ecommerce.catalogservice.service.impl.ProductSearchEventReplayService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    private final ProductSearchDltReplayService dltReplayService;

    private final ProductSearchEventReplayService eventReplayService;


    @GetMapping("/search")
    public Page<ProductSearchResult> search(

            @RequestParam(required = false)
            @Size(
                    max = 200,
                    message = "Từ khóa tìm kiếm không được vượt quá 200 ký tự"
            )
            String keyword,

            @RequestParam(required = false)
            String categoryId,

            @RequestParam(required = false)
            String brandId,

            @RequestParam(required = false)
            String status,

            @RequestParam(required = false)
            @DecimalMin(
                    value = "0.0",
                    message = "minRating phải lớn hơn hoặc bằng 0"
            )
            @DecimalMax(
                    value = "5.0",
                    message = "minRating không được vượt quá 5"
            )
            Double minRating,

            @RequestParam(required = false)
            Boolean buyable,

            @RequestParam(defaultValue = "relevance")
            String sort,

            @RequestParam(defaultValue = "0")
            @Min(
                    value = 0,
                    message = "page phải lớn hơn hoặc bằng 0"
            )
            int page,

            @RequestParam(defaultValue = "20")
            @Min(
                    value = 1,
                    message = "size phải lớn hơn 0"
            )
            @Max(
                    value = 100,
                    message = "size không được vượt quá 100"
            )
            int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        return productSearchService.search(keyword,
                categoryId,
                brandId,
                status,
                minRating,
                buyable,
                sort,
                pageable
        );
    }



    @GetMapping("/search/cursor")
    public CursorSearchResponse searchAfter(

            @RequestParam(required = false)
            @Size(
                    max = 200,
                    message = "Từ khóa tìm kiếm không được vượt quá 200 ký tự"
            )
            String keyword,

            @RequestParam(required = false)
            String categoryId,

            @RequestParam(required = false)
            String brandId,

            @RequestParam(required = false)
            String status,

            @RequestParam(required = false)
            @DecimalMin(
                    value = "0.0",
                    message = "minRating phải lớn hơn hoặc bằng 0"
            )
            @DecimalMax(
                    value = "5.0",
                    message = "minRating không được vượt quá 5"
            )
            Double minRating,

            @RequestParam(required = false)
            Boolean buyable,

            @RequestParam(defaultValue = "relevance")
            String sort,

            @RequestParam(required = false)
            @Size(
                    max = 8192,
                    message = "cursor không được vượt quá 8192 ký tự"
            )
            String cursor,

            @RequestParam(defaultValue = "20")
            @Min(
                    value = 1,
                    message = "size phải lớn hơn 0"
            )
            @Max(
                    value = 100,
                    message = "size không được vượt quá 100"
            )
            int size
    ) {

        return productSearchService.searchAfter(
                keyword,
                categoryId,
                brandId,
                status,
                minRating,
                buyable,
                sort,
                cursor,
                size
        );
    }



    @PostMapping(
            "/search/dlt/{partition}/{offset}/replay"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public SearchDltReplayResponse replayDeadLetter(
            @PathVariable int partition,
            @PathVariable long offset
    ) {

        return dltReplayService.replay(
                partition,
                offset
        );
    }


    @PostMapping("/search/reindex/events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductSearchReplayStatus>
    replayRetainedEvents() {

        return ResponseEntity
                .accepted()
                .body(
                        eventReplayService.start()
                );
    }


    @GetMapping(
            "/search/reindex/events/{runId}"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ProductSearchReplayStatus replayStatus(
            @PathVariable UUID runId
    ) {

        return eventReplayService.status(runId);
    }
}