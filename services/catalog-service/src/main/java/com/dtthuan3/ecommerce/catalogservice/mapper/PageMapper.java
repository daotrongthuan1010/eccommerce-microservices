package com.dtthuan3.ecommerce.catalogservice.mapper;


import com.dtthuan3.ecommerce.catalogservice.dto.response.PageResponse;
import org.springframework.data.domain.Page;

import java.util.function.Function;

public final class PageMapper {

    private PageMapper() {
    }

    public static <E, D> PageResponse<D> toResponse(
            Page<E> page,
            Function<E, D> mapper
    ) {
        return PageResponse.<D>builder()
                .content(
                        page.getContent()
                                .stream()
                                .map(mapper)
                                .toList()
                )
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
