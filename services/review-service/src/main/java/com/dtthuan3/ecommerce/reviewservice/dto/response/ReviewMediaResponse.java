package com.dtthuan3.ecommerce.reviewservice.dto.response;

import com.dtthuan3.ecommerce.reviewservice.entity.enums.ReviewMediaType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewMediaResponse {

    private Long id;

    private ReviewMediaType mediaType;

    private String mediaUrl;

    private String thumbnailUrl;

    private Integer sortOrder;
}