package com.dtthuan3.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeOptionRequest {

    @NotNull(message = "ID thuộc tính không được để trống")
    @Positive(message = "ID thuộc tính phải lớn hơn 0")
    private Long attributeId;

    @NotBlank(message = "Giá trị tùy chọn không được để trống")
    @Size(max = 255,
            message = "Giá trị tùy chọn không được vượt quá 255 ký tự")
    private String value;

    @NotNull(message = "Thứ tự hiển thị không được để trống")
    @PositiveOrZero(message = "Thứ tự hiển thị phải lớn hơn hoặc bằng 0")
    private Integer displayOrder;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean active;
}