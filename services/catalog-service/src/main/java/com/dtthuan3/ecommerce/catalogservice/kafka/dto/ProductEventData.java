package com.dtthuan3.ecommerce.catalogservice.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductEventData {

    private String sku;

    private String name;

    private BigDecimal price;

    private String status;
}