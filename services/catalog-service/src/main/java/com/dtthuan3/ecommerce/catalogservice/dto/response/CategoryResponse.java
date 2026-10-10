package com.dtthuan3.ecommerce.catalogservice.dto.response;

import com.dtthuan3.ecommerce.catalogservice.contstant.CategoryStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CategoryResponse {

    // ID danh mục
    private String id;

    // ID danh mục cha
    private String parentId;

    // Tên danh mục
    private String name;

    // Slug
    private String slug;

    // Mô tả
    private String description;

    // Object key ảnh trên MinIO
    private String imageObjectKey;

    // Thứ tự hiển thị
    private Integer displayOrder;

    // Trạng thái danh mục
    private CategoryStatus status;

    // Phiên bản hiện tại
    private Long revision;


}
