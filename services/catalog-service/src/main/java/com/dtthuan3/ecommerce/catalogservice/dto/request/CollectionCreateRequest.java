package com.dtthuan3.ecommerce.catalogservice.dto.request;

import com.dtthuan3.ecommerce.catalogservice.contstant.CollectionStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CollectionCreateRequest {

    @NotBlank(message = "Tên collection không được để trống")
    @Size(
            max = 255,
            message = "Tên collection không được vượt quá 255 ký tự"
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
            message = "Banner object key không được vượt quá 500 ký tự"
    )
    private String bannerObjectKey;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Min(
            value = 0,
            message = "Display order không được nhỏ hơn 0"
    )
    private Integer displayOrder;

    private CollectionStatus status;
}