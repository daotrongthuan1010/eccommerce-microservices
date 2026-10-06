package com.dtthuan3.ecommerce.productservice.dto.response;
import com.dtthuan3.ecommerce.productservice.constant.ProductStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttributeValueResponse {

    private Long id;

    private Long productId;

    private Long attributeId;

    private String valueText;

    private BigDecimal valueNumber;

    private LocalDate valueDate;

    private Boolean valueBoolean;

    private Long optionId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
