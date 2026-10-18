package com.dtthuan3.ecommerce.catalogservice.dto.request;

import com.dtthuan3.ecommerce.catalogservice.contstant.ManufacturerStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManufacturerUpdateRequest {

    @NotBlank(
            message = "Tên nhà sản xuất không được để trống"
    )
    @Size(
            max = 255,
            message = "Tên nhà sản xuất không được vượt quá 255 ký tự"
    )
    private String name;

    @Size(
            max = 255,
            message = "Quốc gia không được vượt quá 255 ký tự"
    )
    private String country;

    @Size(
            max = 500,
            message = "Website không được vượt quá 500 ký tự"
    )
    private String website;

    private String description;

    @Size(
            max = 500,
            message = "Logo object key không được vượt quá 500 ký tự"
    )
    private String logoObjectKey;

    private ManufacturerStatus status;
}