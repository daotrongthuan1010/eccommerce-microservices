package com.dtthuan3.ecommerce.catalogservice.dto.request;

import com.dtthuan3.ecommerce.catalogservice.contstant.CategoryStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryUpdateRequest {

    // ID danh mục cha
    private String parentId;

    // Tên danh mục
    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(
            max = 255,
            message = "Tên danh mục không được vượt quá 255 ký tự"
    )
    private String name;

    // Slug của danh mục
    @NotBlank(message = "Slug không được để trống")
    @Size(
            max = 255,
            message = "Slug không được vượt quá 255 ký tự"
    )
    private String slug;

    // Mô tả danh mục
    private String description;

    // Object key ảnh trên MinIO
    @Size(
            max = 500,
            message = "Object key không được vượt quá 500 ký tự"
    )
    private String imageObjectKey;

    // Thứ tự hiển thị
    @Min(
            value = 0,
            message = "Thứ tự hiển thị không được nhỏ hơn 0"
    )
    private Integer displayOrder;

    // Trạng thái danh mục
    private CategoryStatus status;
}