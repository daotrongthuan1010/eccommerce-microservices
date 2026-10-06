package com.dtthuan3.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "Mã sản phẩm không được để trống")
    @Size(max = 100, message = "Mã sản phẩm không được vượt quá 100 ký tự")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]+$",
            message = "Mã sản phẩm chỉ được chứa chữ cái, số, dấu gạch ngang và dấu gạch dưới"
    )
    private String productCode;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(min = 2, max = 255,
            message = "Tên sản phẩm phải từ 2 đến 255 ký tự")
    private String name;

    @Size(max = 500,
            message = "Mô tả ngắn không được vượt quá 500 ký tự")
    private String shortDescription;

    @Size(max = 5000,
            message = "Mô tả không được vượt quá 5000 ký tự")
    private String description;

    @Size(max = 100,
            message = "Nước xuất xứ không được vượt quá 100 ký tự")
    private String originCountry;

    @NotNull(message = "ID danh mục không được để trống")
    @Positive(message = "ID danh mục phải lớn hơn 0")
    private Long categoryId;

    @NotNull(message = "ID thương hiệu không được để trống")
    @Positive(message = "ID thương hiệu phải lớn hơn 0")
    private Long brandId;

    @NotNull(message = "ID nhà sản xuất không được để trống")
    @Positive(message = "ID nhà sản xuất phải lớn hơn 0")
    private Long manufacturerId;

    @NotNull(message = "ID mẫu thuộc tính không được để trống")
    @Positive(message = "ID mẫu thuộc tính phải lớn hơn 0")
    private Long attributeTemplateId;
}