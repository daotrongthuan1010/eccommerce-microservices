package com.dtthuan3.ecommerce.catalogservice.dto.response;

import com.dtthuan3.ecommerce.catalogservice.contstant.CategoryStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CategoryTreeResponse {

    private String id;

    private String parentId;

    private String name;

    private String slug;

    private String imageObjectKey;

    private Integer displayOrder;

    private CategoryStatus status;

    private List<CategoryTreeResponse> children;
}