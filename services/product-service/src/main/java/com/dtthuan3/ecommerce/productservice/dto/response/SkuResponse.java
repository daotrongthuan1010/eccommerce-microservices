package com.dtthuan3.ecommerce.productservice.dto.response;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkuResponse {

    private UUID id;

    private UUID variantId;

    private String skuCode;

    private String barcode;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
