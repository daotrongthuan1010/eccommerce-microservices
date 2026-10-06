package com.dtthuan3.ecommerce.productservice.dto.response;
import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantResponse {

    private Long id;

    private Long productId;

    private String variantCode;

    private String name;

    private String description;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
