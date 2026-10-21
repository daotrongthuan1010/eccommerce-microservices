package com.dtthuan3.ecommerce.catalogservice.mapper;

import com.dtthuan3.ecommerce.catalogservice.dto.request.ManufacturerCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.ManufacturerUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ManufacturerResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.Manufacturers;
import org.springframework.stereotype.Component;

@Component
public class ManufacturerMapper {

    public Manufacturers toEntity(
            ManufacturerCreateRequest request
    ) {

        Manufacturers manufacturer = new Manufacturers();

        manufacturer.setName(request.getName());
        manufacturer.setCountry(request.getCountry());
        manufacturer.setWebsite(request.getWebsite());
        manufacturer.setDescription(request.getDescription());
        manufacturer.setLogoObjectKey(request.getLogoObjectKey());
        manufacturer.setStatus(request.getStatus());

        return manufacturer;
    }

    public void updateEntity(
            ManufacturerUpdateRequest request,
            Manufacturers manufacturer
    ) {

        manufacturer.setName(request.getName());
        manufacturer.setCountry(request.getCountry());
        manufacturer.setWebsite(request.getWebsite());
        manufacturer.setDescription(request.getDescription());
        manufacturer.setLogoObjectKey(request.getLogoObjectKey());

        if (request.getStatus() != null) {
            manufacturer.setStatus(request.getStatus());
        }
    }

    public ManufacturerResponse toResponse(
            Manufacturers manufacturer
    ) {

        return ManufacturerResponse.builder()
                .id(manufacturer.getId())
                .name(manufacturer.getName())
                .country(manufacturer.getCountry())
                .website(manufacturer.getWebsite())
                .description(manufacturer.getDescription())
                .logoObjectKey(manufacturer.getLogoObjectKey())
                .status(manufacturer.getStatus())
                .revision(manufacturer.getRevision())
                .build();
    }
}