package com.dtthuan3.ecommerce.catalogservice.dto.request;


import com.dtthuan3.ecommerce.catalogservice.contstant.BrandsStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandsCreateRequest {

    @NotBlank(message = "Tên thương hiệu không được để trống")
    @Size(
            max = 255,
            message = "Tên thương hiệu không được vượt quá 255 ký tự"
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
            message = "Logo object key không được vượt quá 500 ký tự"
    )
    private String logoObjectKey;

    @Size(
            max = 500,
            message = "Website không được vượt quá 500 ký tự"
    )
    private String website;

    @NotNull(message = "Trạng thái không được để trống")
    private BrandsStatus status = BrandsStatus.ACTIVE;
}