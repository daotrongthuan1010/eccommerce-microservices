package com.dtthuan3.ecommerce.productservice.dto.response;

import com.dtthuan3.ecommerce.productservice.constant.ProductStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProductResponse {

    private UUID id;

    private String productCode;

    private UUID categoryId;

    private UUID brandId;

    private UUID manufacturerId;

    private UUID attributeTemplateId;

    private UUID currentRevisionId;

    private UUID publishedRevisionId;

    private ProductStatus status;

    private ProductRevisionResponse currentRevision;
}