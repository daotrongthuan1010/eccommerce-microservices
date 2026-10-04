package com.dtthuan3.ecommerce.catalogservice.dto.response;

import com.dtthuan3.ecommerce.catalogservice.contstant.BrandsStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BrandsRespone {

    private Long id;

    private String name;

    private String slug;

    private String description;

    private String logoUrl;

    private BrandsStatus status;
}