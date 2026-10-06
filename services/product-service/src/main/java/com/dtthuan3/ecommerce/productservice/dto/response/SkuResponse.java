package com.dtthuan3.ecommerce.productservice.dto.response;
import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkuResponse {

    private Long id;

    private Long variantId;

    private String skuCode;

    private String barcode;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
