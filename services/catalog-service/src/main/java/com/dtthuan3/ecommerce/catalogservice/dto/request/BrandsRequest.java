package com.dtthuan3.ecommerce.catalogservice.dto.request;

import com.dtthuan3.ecommerce.catalogservice.contstant.BrandsStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BrandsRequest {

    @NotBlank(
            message = "Tên thương hiệu không được để trống"
    )
    @Size(
            max = 255,
            message = "Tên thương hiệu không vượt quá 255 ký tự"
    )
    private String name;

    @NotBlank(
            message = "Slug không được để trống"
    )
    @Size(
            max = 255,
            message = "Slug không vượt quá 255 ký tự"
    )
    private String slug;

    private String description;

    @NotNull(
            message = "Trạng thái không được để trống"
    )
    private BrandsStatus status;
}