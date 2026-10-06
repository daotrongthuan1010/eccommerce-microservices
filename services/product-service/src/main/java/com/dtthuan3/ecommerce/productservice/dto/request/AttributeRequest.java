package com.dtthuan3.ecommerce.productservice.dto.request;

import com.dtthuan3.ecommerce.productservice.constant.AttributeDataType;
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
public class AttributeRequest {

    @NotBlank(message = "Tên thuộc tính không được để trống")
    @Size(min = 2, max = 100,
            message = "Tên thuộc tính phải từ 2 đến 100 ký tự")
    private String name;

    @NotBlank(message = "Mã thuộc tính không được để trống")
    @Size(max = 100,
            message = "Mã thuộc tính không được vượt quá 100 ký tự")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]+$",
            message = "Mã thuộc tính chỉ được chứa chữ cái, số, dấu gạch ngang và dấu gạch dưới"
    )
    private String code;

    @NotNull(message = "Kiểu dữ liệu thuộc tính không được để trống")
    private AttributeDataType dataType;


    @Size(max = 50,
            message = "Đơn vị không được vượt quá 50 ký tự")
    private String unit;

    @NotNull(message = "Trạng thái bắt buộc không được để trống")
    private Boolean required;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean active;
}