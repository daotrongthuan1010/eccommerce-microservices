package com.dtthuan3.ecommerce.catalogservice.dto.response;


import com.dtthuan3.ecommerce.catalogservice.contstant.BrandsStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BrandsResponse {

    private String id;

    private String name;

    private String slug;

    private String description;

    private String logoObjectKey;

    private String website;

    private BrandsStatus status;

    private Long revision;
}