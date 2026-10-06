package com.dtthuan3.ecommerce.productservice.dto.response;
import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeOptionResponse {

    private Long id;

    private Long attributeId;

    private String value;

    private Integer displayOrder;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
