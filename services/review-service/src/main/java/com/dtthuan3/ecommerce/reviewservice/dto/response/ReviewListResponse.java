package com.dtthuan3.ecommerce.reviewservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewListResponse {

    private Long id;

    private String displayUserName;

    private Integer rating;

    private String comment;

    private boolean verifiedPurchase;

    private boolean anonymous;

    private Long helpfulCount;

    private String sellerReply;

    private List<ReviewMediaResponse> media;

    private LocalDateTime createdAt;
}