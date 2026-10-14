package com.dtthuan3.ecommerce.catalogservice.dto.response;

import com.dtthuan3.ecommerce.catalogservice.contstant.ManufacturerStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ManufacturersRespone {

    private Long id;

    private String name;

    private String country;

    private String description;

    private String website;

    private ManufacturerStatus status;
}