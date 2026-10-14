package com.dtthuan3.ecommerce.catalogservice.dto.response;

import com.dtthuan3.ecommerce.catalogservice.contstant.CategoryStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CategoryResponse {

    private Long id;

    private Long parentId;

    private String name;

    private String slug;

    private String imageUrl;

    private Integer displayOrder;

    private CategoryStatus status;


}
