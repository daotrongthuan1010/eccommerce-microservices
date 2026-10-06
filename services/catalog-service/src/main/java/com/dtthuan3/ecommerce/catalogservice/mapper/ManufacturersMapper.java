package com.dtthuan3.ecommerce.catalogservice.mapper;

import com.dtthuan3.ecommerce.catalogservice.dto.request.ManufacturersRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ManufacturersRespone;
import com.dtthuan3.ecommerce.catalogservice.entity.Manufacturers;
import org.springframework.stereotype.Component;

@Component
public class ManufacturersMapper {

    public Manufacturers toEntity(
            ManufacturersRequest request
    ) {

        Manufacturers manufacturers =
                new Manufacturers();

        manufacturers.setName(request.getName());
        manufacturers.setCountry(request.getCountry());
        manufacturers.setDescription(
                request.getDescription()
        );
        manufacturers.setWebsite(
                request.getWebsite()
        );
        manufacturers.setStatus(
                request.getStatus()
        );

        return manufacturers;
    }

    public void updateEntity(
            Manufacturers manufacturers,
            ManufacturersRequest request
    ) {

        manufacturers.setName(request.getName());
        manufacturers.setCountry(request.getCountry());
        manufacturers.setDescription(
                request.getDescription()
        );
        manufacturers.setWebsite(
                request.getWebsite()
        );
        manufacturers.setStatus(
                request.getStatus()
        );
    }

    public ManufacturersRespone toResponse(
            Manufacturers manufacturers
    ) {

        return ManufacturersRespone.builder()
                .id(manufacturers.getId())
                .name(manufacturers.getName())
                .country(manufacturers.getCountry())
                .description(
                        manufacturers.getDescription()
                )
                .website(
                        manufacturers.getWebsite()
                )
                .status(
                        manufacturers.getStatus()
                )
                .build();
    }
}