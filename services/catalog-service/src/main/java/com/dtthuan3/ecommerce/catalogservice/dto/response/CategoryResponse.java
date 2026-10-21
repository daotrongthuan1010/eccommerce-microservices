package com.dtthuan3.ecommerce.catalogservice.dto.response;

import com.dtthuan3.ecommerce.catalogservice.contstant.CategoryStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryResponse {

    private String id;

    private String parentId;

    private String name;

    private String slug;

    private String description;

    private String imageObjectKey;

    private Integer displayOrder;

    private CategoryStatus status;

    private Long revision;
}