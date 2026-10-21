package com.dtthuan3.ecommerce.catalogservice.mapper;

import com.dtthuan3.ecommerce.catalogservice.dto.request.SynonymCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.SynonymSetCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.SynonymResponse;
import com.dtthuan3.ecommerce.catalogservice.dto.response.SynonymSetResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.Synonym;
import com.dtthuan3.ecommerce.catalogservice.entity.SynonymSet;
import org.springframework.stereotype.Component;

@Component
public class SynonymMapper {

    public SynonymSet toEntity(
            SynonymSetCreateRequest request
    ) {

        SynonymSet entity = new SynonymSet();

        entity.setName(request.getName());
        entity.setStatus(request.getStatus());

        return entity;
    }

    public SynonymSetResponse toResponse(
            SynonymSet entity
    ) {

        return SynonymSetResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .version(entity.getVersion())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    public Synonym toEntity(
            SynonymCreateRequest request
    ) {

        Synonym entity = new Synonym();

        entity.setTerm(request.getTerm());
        entity.setSynonym(request.getSynonym());

        return entity;
    }

    public SynonymResponse toResponse(
            Synonym entity
    ) {

        return SynonymResponse.builder()
                .id(entity.getId())
                .synonymSetId(entity.getSynonymSetId())
                .term(entity.getTerm())
                .synonym(entity.getSynonym())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}