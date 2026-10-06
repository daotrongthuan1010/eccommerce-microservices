package com.dtthuan3.ecommerce.productservice.dto.request;

import com.dtthuan3.ecommerce.productservice.constant.ProductStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPublishingHistoryRequest {

    @NotNull(message = "ID sản phẩm không được để trống")
    private Long productId;

    @NotNull(message = "Trạng thái cũ không được để trống")
    private ProductStatus fromStatus;

    @NotNull(message = "Trạng thái mới không được để trống")
    private ProductStatus toStatus;

    @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự")
    private String reason;

    @NotNull(message = "ID người thực hiện không được để trống")
    private Long changedBy;
}