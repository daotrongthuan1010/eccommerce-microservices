package com.dtthuan3.ecommerce.catalogservice.dto.search;

import java.util.List;

/** Cursor-based result that avoids exact total-hit counting and deep offsets. */
public record CursorSearchResponse(
        List<ProductSearchResult> items,
        String nextCursor,
        boolean hasMore
) {
}
