package com.dtthuan3.ecommerce.catalogservice.client.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ProductRevisionResponse {
    private UUID id;
    private Long version;
    private String name;
    private String shortDescription;
    private String description;
    private String originCountry;
    private String storageCondition;
    private String deliveryCondition;
    private String returnPolicy;
}
