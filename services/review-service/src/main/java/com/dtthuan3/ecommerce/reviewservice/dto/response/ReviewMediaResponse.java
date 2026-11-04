package com.dtthuan3.ecommerce.reviewservice.dto.response;

import com.dtthuan3.ecommerce.reviewservice.enums.ReviewMediaType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewMediaResponse {
    private String fileId;
    private ReviewMediaType mediaType;
    private String mediaUrl;
    private String thumbnailUrl;
    private Integer sortOrder;
}
