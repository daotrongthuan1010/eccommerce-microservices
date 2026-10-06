package com.dtthuan3.ecommerce.productservice.dto.response;
import com.dtthuan3.ecommerce.productservice.constant.ProductStatus;
import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPublishingHistoryResponse {

    private Long id;

    private Long productId;

    private ProductStatus fromStatus;

    private ProductStatus toStatus;

    private String reason;

    private Long changedBy;

    private LocalDateTime changedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
