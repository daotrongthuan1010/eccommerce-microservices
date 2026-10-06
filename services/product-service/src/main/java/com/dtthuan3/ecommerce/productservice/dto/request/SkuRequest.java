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
public class SkuRequest {

    @NotNull(message = "ID biến thể không được để trống")
    @Positive(message = "ID biến thể phải lớn hơn 0")
    private Long variantId;

    @NotBlank(message = "Mã SKU không được để trống")
    @Size(max = 100,
            message = "Mã SKU không được vượt quá 100 ký tự")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]+$",
            message = "Mã SKU chỉ được chứa chữ cái, số, dấu gạch ngang và dấu gạch dưới"
    )
    private String skuCode;

    @NotBlank(message = "Mã vạch không được để trống")
    @Size(min = 8, max = 100,
            message = "Mã vạch phải từ 8 đến 100 ký tự")
    @Pattern(
            regexp = "^[0-9]+$",
            message = "Mã vạch chỉ được chứa chữ số"
    )
    private String barcode;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean active;
}