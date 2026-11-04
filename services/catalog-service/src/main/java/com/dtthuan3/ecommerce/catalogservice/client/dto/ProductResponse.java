package com.dtthuan3.ecommerce.catalogservice.client.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ProductResponse {
    private UUID id;
    private String productCode;
    private UUID categoryId;
    private UUID brandId;
    private UUID manufacturerId;
    private UUID attributeTemplateId;
    private UUID currentRevisionId;
    private UUID publishedRevisionId;
    private String status;
    private ProductRevisionResponse currentRevision;
}