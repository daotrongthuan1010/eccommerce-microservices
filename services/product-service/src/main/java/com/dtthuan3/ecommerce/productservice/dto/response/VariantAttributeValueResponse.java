package com.dtthuan3.ecommerce.productservice.dto.response;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantAttributeValueResponse {

    private UUID id;

    private UUID variantId;

    private UUID attributeId;

    private String valueText;

    private BigDecimal valueNumber;

    private LocalDate valueDate;

    private Boolean valueBoolean;

    private UUID optionId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
