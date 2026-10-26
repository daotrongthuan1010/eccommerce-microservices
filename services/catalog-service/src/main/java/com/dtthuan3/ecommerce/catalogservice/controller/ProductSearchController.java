package com.dtthuan3.ecommerce.catalogservice.controller;

import com.dtthuan3.ecommerce.catalogservice.dto.search.ProductSearchResult;
import com.dtthuan3.ecommerce.catalogservice.dto.search.CursorSearchResponse;
import com.dtthuan3.ecommerce.catalogservice.dto.search.SearchDltReplayResponse;
import com.dtthuan3.ecommerce.catalogservice.dto.search.ProductSearchReplayStatus;
import com.dtthuan3.ecommerce.catalogservice.service.impl.ProductSearchDltReplayService;
import com.dtthuan3.ecommerce.catalogservice.service.impl.ProductSearchEventReplayService;
import com.dtthuan3.ecommerce.catalogservice.service.ProductSearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
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
            @RequestParam @NotBlank(message = "Từ khóa tìm kiếm không được để trống")
            @Size(max = 160, message = "Từ khóa tìm kiếm không được vượt quá 160 ký tự")
            String keyword,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page phải lớn hơn hoặc bằng 0")
            @Max(value = 100, message = "Chỉ cho phép truy vấn tối đa đến page 100") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size phải lớn hơn 0")
            @Max(value = 100, message = "size không được vượt quá 100") int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        return productSearchService.search(
                keyword,
                pageable
        );
    }

    /** Cursor paging for large catalogs; the existing Page endpoint stays compatible. */
    @GetMapping("/search/cursor")
    public CursorSearchResponse searchAfter(
            @RequestParam @NotBlank @Size(max = 160) String keyword,
            @RequestParam(required = false) @Size(max = 8192) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return productSearchService.searchAfter(keyword, cursor, size);
    }

    @PostMapping("/search/dlt/{partition}/{offset}/replay")
    @PreAuthorize("hasRole('ADMIN')")
    public SearchDltReplayResponse replayDeadLetter(
            @PathVariable int partition,
            @PathVariable long offset
    ) {
        return dltReplayService.replay(partition, offset);
    }

    @PostMapping("/search/reindex/events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductSearchReplayStatus> replayRetainedEvents() {
        return ResponseEntity.accepted().body(eventReplayService.start());
    }

    @GetMapping("/search/reindex/events/{runId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductSearchReplayStatus replayStatus(@PathVariable UUID runId) {
        return eventReplayService.status(runId);
    }
}
