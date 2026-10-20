package com.dtthuan3.ecommerce.catalogservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ProductCategoryUpdateRequest(

        @NotEmpty(message = "Category ids must not be empty")
        List<@NotNull(message = "Category id must not be null") String> categoryIds,

        @NotNull(message = "Primary category id must not be null")
        String primaryCategoryId

) {
}