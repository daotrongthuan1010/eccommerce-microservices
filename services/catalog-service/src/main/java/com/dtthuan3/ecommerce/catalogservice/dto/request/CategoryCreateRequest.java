package com.dtthuan3.ecommerce.catalogservice.dto.request;

import com.dtthuan3.ecommerce.catalogservice.contstant.CategoryStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryCreateRequest {

    private String parentId;

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(
            max = 255,
            message = "Tên danh mục không được vượt quá 255 ký tự"
    )
    private String name;

    @NotBlank(message = "Slug không được để trống")
    @Size(
            max = 255,
            message = "Slug không được vượt quá 255 ký tự"
    )
    private String slug;

    private String description;

    @Size(
            max = 500,
            message = "Object key không được vượt quá 500 ký tự"
    )
    private String imageObjectKey;

    @Min(
            value = 0,
            message = "Thứ tự hiển thị không được nhỏ hơn 0"
    )
    private Integer displayOrder = 0;

    @NotNull(message = "Trạng thái không được để trống")
    private CategoryStatus status = CategoryStatus.ACTIVE;
}