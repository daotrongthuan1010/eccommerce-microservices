package com.dtthuan3.ecommerce.productservice.dto.response;
import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeTemplateItemResponse {

    private Long id;

    private Long templateId;

    private Long attributeId;

    private Integer displayOrder;

    private Boolean required;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
