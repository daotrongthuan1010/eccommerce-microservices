package com.dtthuan3.ecommerce.productservice.dto.response;

import com.dtthuan3.ecommerce.productservice.constant.ProductStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;

    private String productCode;

    private String name;

    private String shortDescription;

    private String description;

    private String originCountry;

    private Long categoryId;

    private Long brandId;

    private Long manufacturerId;

    private Long attributeTemplateId;

    private ProductStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}