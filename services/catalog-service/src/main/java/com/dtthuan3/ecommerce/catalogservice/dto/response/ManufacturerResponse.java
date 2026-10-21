package com.dtthuan3.ecommerce.catalogservice.dto.response;

import com.dtthuan3.ecommerce.catalogservice.contstant.ManufacturerStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ManufacturerResponse {

    private String id;

    private String name;

    private String country;

    private String website;

    private String description;

    private String logoObjectKey;

    private ManufacturerStatus status;

    private Long revision;
}