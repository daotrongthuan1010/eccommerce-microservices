package com.dtthuan3.ecommerce.productservice.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantAttributeValueRequest {

    @NotNull(message = "ID biến thể không được để trống")
    @Positive(message = "ID biến thể phải lớn hơn 0")
    private Long variantId;

    @NotNull(message = "ID thuộc tính không được để trống")
    @Positive(message = "ID thuộc tính phải lớn hơn 0")
    private Long attributeId;

    @Size(
            max = 1000,
            message = "Giá trị văn bản không được vượt quá 1000 ký tự"
    )
    private String valueText;

    @Digits(
            integer = 14,
            fraction = 6,
            message = "Giá trị số không hợp lệ, tối đa 14 chữ số phần nguyên và 6 chữ số thập phân"
    )
    private BigDecimal valueNumber;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate valueDate;

    private Boolean valueBoolean;

    @Positive(message = "ID tùy chọn phải lớn hơn 0")
    private Long optionId;
}