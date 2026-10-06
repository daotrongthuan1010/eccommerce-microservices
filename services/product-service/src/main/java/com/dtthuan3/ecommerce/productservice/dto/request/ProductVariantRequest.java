package com.dtthuan3.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantRequest {

    @NotNull(message = "ID sản phẩm không được để trống")
    @Positive(message = "ID sản phẩm phải lớn hơn 0")
    private Long productId;

    @NotBlank(message = "Mã biến thể không được để trống")
    @Size(max = 100,
            message = "Mã biến thể không được vượt quá 100 ký tự")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]+$",
            message = "Mã biến thể chỉ được chứa chữ cái, số, dấu gạch ngang và dấu gạch dưới"
    )
    private String variantCode;

    @NotBlank(message = "Tên biến thể không được để trống")
    @Size(min = 1, max = 255,
            message = "Tên biến thể không được vượt quá 255 ký tự")
    private String name;

    @Size(max = 5000,
            message = "Mô tả không được vượt quá 5000 ký tự")
    private String description;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean active;
}