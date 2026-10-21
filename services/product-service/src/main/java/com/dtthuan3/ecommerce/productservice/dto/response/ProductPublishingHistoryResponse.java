package com.dtthuan3.ecommerce.productservice.dto.response;
import com.dtthuan3.ecommerce.productservice.constant.ProductStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPublishingHistoryResponse {

    private UUID id;

    private UUID productId;

    private ProductStatus fromStatus;

    private ProductStatus toStatus;

    private String reason;

    private UUID changedBy;

    private LocalDateTime changedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
