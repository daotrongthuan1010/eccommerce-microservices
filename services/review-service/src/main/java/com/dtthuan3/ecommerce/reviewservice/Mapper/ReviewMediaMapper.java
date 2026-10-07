package com.dtthuan3.ecommerce.reviewservice.Mapper;

import com.dtthuan3.ecommerce.reviewservice.dto.request.ReviewMediaRequest;
import com.dtthuan3.ecommerce.reviewservice.dto.response.ReviewMediaResponse;
import com.dtthuan3.ecommerce.reviewservice.entity.ReviewMedia;

import org.springframework.stereotype.Component;

@Component
public class ReviewMediaMapper {
    public ReviewMedia toEntity(ReviewMediaRequest request, Long reviewId, Integer defaultSortOrder) {
        if (request == null) {
            return null;
        }
        return ReviewMedia.builder()
                .reviewId(reviewId)
                .mediaType(request.getMediaType())
                .mediaUrl(request.getMediaUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .sortOrder(
                        request.getSortOrder() != null
                                ? request.getSortOrder()
                                : defaultSortOrder
                )
                .build();
    }


    public ReviewMediaResponse toResponse(ReviewMedia media) {
        if (media == null) {return null;}
        return ReviewMediaResponse.builder()
                .id(media.getId())
                .mediaType(media.getMediaType())
                .mediaUrl(media.getMediaUrl())
                .thumbnailUrl(media.getThumbnailUrl())
                .sortOrder(media.getSortOrder())
                .build();
    }
}