package com.dtthuan3.ecommerce.catalogservice.dto.request;

import com.dtthuan3.ecommerce.catalogservice.contstant.ManufacturerStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ManufacturersRequest {

    @NotBlank(message = "Tên nhà sản xuất không được để trống")
    @Size(
            max = 255,
            message = "Tên nhà sản xuất không vượt quá 255 ký tự"
    )
    private String name;

    @NotBlank(message = "Quốc gia không được để trống")
    @Size(
            max = 50,
            message = "Quốc gia không vượt quá 50 ký tự"
    )
    private String country;

    private String description;

    @Size(
            max = 500,
            message = "Website không vượt quá 500 ký tự"
    )
    private String website;

    @NotNull(message = "Trạng thái không được để trống")
    private ManufacturerStatus status;
}