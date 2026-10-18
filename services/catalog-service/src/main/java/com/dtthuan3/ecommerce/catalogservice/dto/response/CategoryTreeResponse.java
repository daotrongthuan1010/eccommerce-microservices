package com.dtthuan3.ecommerce.catalogservice.dto.response;

import com.dtthuan3.ecommerce.catalogservice.contstant.CategoryStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class CategoryTreeResponse {

    private String id;

    private String parentId;

    private String name;

    private String slug;

    private CategoryStatus status;

    private Integer displayOrder;

    @Builder.Default
    private List<CategoryTreeResponse> children = new ArrayList<>();
}