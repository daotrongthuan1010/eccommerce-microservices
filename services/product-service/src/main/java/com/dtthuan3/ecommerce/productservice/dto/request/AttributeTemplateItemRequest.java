package com.dtthuan3.ecommerce.productservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeTemplateItemRequest {

    @NotNull(message = "ID mẫu thuộc tính không được để trống")
    @Positive(message = "ID mẫu thuộc tính phải lớn hơn 0")
    private Long templateId;

    @NotNull(message = "ID thuộc tính không được để trống")
    @Positive(message = "ID thuộc tính phải lớn hơn 0")
    private Long attributeId;

    @NotNull(message = "Thứ tự hiển thị không được để trống")
    @PositiveOrZero(message = "Thứ tự hiển thị phải lớn hơn hoặc bằng 0")
    private Integer displayOrder;

    @NotNull(message = "Trạng thái bắt buộc không được để trống")
    private Boolean required;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean active;
}