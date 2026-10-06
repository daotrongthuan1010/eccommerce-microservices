package com.dtthuan3.ecommerce.productservice.dto.response;
import com.dtthuan3.ecommerce.productservice.constant.MediaType;
import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductMediaResponse {

    private Long id;

    private Long productId;

    private MediaType mediaType;

    private String storageKey;

    private String fileName;

    private String contentType;

    private Integer displayOrder;

    private Boolean primary;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
