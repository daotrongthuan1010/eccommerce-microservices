package com.dtthuan3.ecommerce.catalogservice.mapper;

import com.dtthuan3.ecommerce.catalogservice.dto.request.CollectionCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.CollectionUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CollectionResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.Collection;
import org.springframework.stereotype.Component;

@Component
public class CollectionMapper {

    public Collection toEntity(
            CollectionCreateRequest request
    ) {

        Collection collection = new Collection();

        collection.setName(request.getName());
        collection.setSlug(request.getSlug());
        collection.setDescription(request.getDescription());
        collection.setBannerObjectKey(
                request.getBannerObjectKey()
        );
        collection.setStartAt(request.getStartAt());
        collection.setEndAt(request.getEndAt());
        collection.setDisplayOrder(
                request.getDisplayOrder()
        );
        collection.setStatus(request.getStatus());

        return collection;
    }

    public void updateEntity(
            CollectionUpdateRequest request,
            Collection collection
    ) {

        collection.setName(request.getName());
        collection.setSlug(request.getSlug());
        collection.setDescription(request.getDescription());
        collection.setBannerObjectKey(
                request.getBannerObjectKey()
        );
        collection.setStartAt(request.getStartAt());
        collection.setEndAt(request.getEndAt());
        collection.setDisplayOrder(
                request.getDisplayOrder()
        );
    }

    public CollectionResponse toResponse(
            Collection collection
    ) {

        return CollectionResponse.builder()
                .id(collection.getId())
                .name(collection.getName())
                .slug(collection.getSlug())
                .description(collection.getDescription())
                .bannerObjectKey(
                        collection.getBannerObjectKey()
                )
                .startAt(collection.getStartAt())
                .endAt(collection.getEndAt())
                .displayOrder(
                        collection.getDisplayOrder()
                )
                .status(collection.getStatus())
                .revision(collection.getRevision())
                .build();
    }
}
