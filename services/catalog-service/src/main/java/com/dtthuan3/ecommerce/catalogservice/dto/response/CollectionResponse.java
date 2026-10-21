package com.dtthuan3.ecommerce.catalogservice.dto.response;

import com.dtthuan3.ecommerce.catalogservice.contstant.CollectionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CollectionResponse {

    private String id;

    private String name;

    private String slug;

    private String description;

    private String bannerObjectKey;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private Integer displayOrder;

    private CollectionStatus status;

    private Long revision;
}