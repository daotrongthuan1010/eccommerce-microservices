package com.dtthuan3.ecommerce.catalogservice.mapper;

import com.dtthuan3.ecommerce.catalogservice.dto.request.BrandsRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.BrandsRespone;
import com.dtthuan3.ecommerce.catalogservice.entity.Brands;
import org.springframework.stereotype.Component;

@Component
public class BrandsMapper {

    /**
     * Request -> Entity
     */
    public Brands toEntity(
            BrandsRequest request
    ) {

        Brands brands = new Brands();

        brands.setName(request.getName());

        brands.setSlug(request.getSlug());

        brands.setDescription(request.getDescription());

        brands.setStatus(request.getStatus());

        return brands;
    }


    /**
     * Update thông tin Brand.
     *
     * Không xử lý logo ở đây.
     */
    public void updateEntity(
            Brands brands,
            BrandsRequest request
    ) {

        brands.setName(request.getName());

        brands.setSlug(request.getSlug());

        brands.setDescription(request.getDescription());

        brands.setStatus(request.getStatus());
    }


    /**
     * Entity -> Response
     */
    public BrandsRespone toResponse(
            Brands brands,
            String logoUrl
    ) {

        return BrandsRespone.builder()
                .id(brands.getId())
                .name(brands.getName())
                .slug(brands.getSlug())
                .description(brands.getDescription())
                .logoUrl(logoUrl)
                .status(brands.getStatus())
                .build();
    }
}