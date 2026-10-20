package com.dtthuan3.ecommerce.catalogservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SynonymResponse {

    private String id;

    private String synonymSetId;

    private String term;

    private String synonym;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}