package com.dtthuan3.ecommerce.productservice.dto.response;
import com.dtthuan3.ecommerce.productservice.constant.AttributeDataType;
import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeResponse {

    private Long id;

    private String name;

    private String code;

    private AttributeDataType dataType;

    private String unit;

    private Boolean required;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
