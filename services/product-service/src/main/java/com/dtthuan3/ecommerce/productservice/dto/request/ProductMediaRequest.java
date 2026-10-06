package com.dtthuan3.ecommerce.productservice.dto.request;

import com.dtthuan3.ecommerce.productservice.constant.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductMediaRequest {

    @NotNull(message = "ID sản phẩm không được để trống")
    @Positive(message = "ID sản phẩm phải lớn hơn 0")
    private Long productId;

    @NotNull(message = "Loại media không được để trống")
    private MediaType mediaType;

    @NotBlank(message = "Storage key không được để trống")
    @Size(max = 500,
            message = "Storage key không được vượt quá 500 ký tự")
    private String storageKey;

    @NotBlank(message = "Tên file không được để trống")
    @Size(max = 255,
            message = "Tên file không được vượt quá 255 ký tự")
    private String fileName;

    @NotBlank(message = "Content type không được để trống")
    @Size(max = 100,
            message = "Content type không được vượt quá 100 ký tự")
    private String contentType;

    @NotNull(message = "Thứ tự hiển thị không được để trống")
    @PositiveOrZero(message = "Thứ tự hiển thị phải lớn hơn hoặc bằng 0")
    private Integer displayOrder;

    @NotNull(message = "Trạng thái media chính không được để trống")
    private Boolean primary;
}