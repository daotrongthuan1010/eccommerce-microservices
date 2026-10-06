package com.dtthuan3.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeTemplateRequest {

    @NotBlank(message = "Tên mẫu thuộc tính không được để trống")
    @Size(min = 2, max = 150,
            message = "Tên mẫu thuộc tính phải từ 2 đến 150 ký tự")
    private String name;

    @NotBlank(message = "Mã mẫu thuộc tính không được để trống")
    @Size(max = 100,
            message = "Mã mẫu thuộc tính không được vượt quá 100 ký tự")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]+$",
            message = "Mã mẫu thuộc tính chỉ được chứa chữ cái, số, dấu gạch ngang và dấu gạch dưới"
    )
    private String code;

    @Size(max = 500,
            message = "Mô tả không được vượt quá 500 ký tự")
    private String description;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean active;
}