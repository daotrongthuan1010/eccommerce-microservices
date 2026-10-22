package com.dtthuan3.ecommerce.productservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
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