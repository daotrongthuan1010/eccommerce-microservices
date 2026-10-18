package com.dtthuan3.ecommerce.catalogservice.mapper;

import com.dtthuan3.ecommerce.catalogservice.dto.request.BrandsCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.BrandsResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.Brands;
import org.springframework.stereotype.Component;

@Component
public class BrandsMapper {

    public Brands toEntity(BrandsCreateRequest request) {

        Brands brands = new Brands();

        brands.setName(request.getName());
        brands.setSlug(request.getSlug());
        brands.setDescription(request.getDescription());
        brands.setLogoObjectKey(request.getLogoObjectKey());
        brands.setWebsite(request.getWebsite());
        brands.setStatus(request.getStatus());
        brands.setRevision(0L);

        return brands;
    }

    public BrandsResponse toResponse(Brands brands) {

        return BrandsResponse.builder()
                .id(brands.getId())
                .name(brands.getName())
                .slug(brands.getSlug())
                .description(brands.getDescription())
                .logoObjectKey(brands.getLogoObjectKey())
                .website(brands.getWebsite())
                .status(brands.getStatus())
                .revision(brands.getRevision())
                .build();
    }
}