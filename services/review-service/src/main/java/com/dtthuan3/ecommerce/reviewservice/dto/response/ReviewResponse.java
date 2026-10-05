package com.dtthuan3.ecommerce.reviewservice.dto.response;

import com.dtthuan3.ecommerce.reviewservice.entity.enums.ReviewStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private Long id;

    private Long userId;

    private String displayUserName;

    private Long orderId;

    private Long orderItemId;

    private Long productId;

    private Long variantId;

    private Integer rating;

    private String comment;

    private boolean verifiedPurchase;

    private boolean anonymous;

    private Long helpfulCount;

    private String sellerReply;

    private LocalDateTime sellerRepliedAt;

    private ReviewStatus status;

    private boolean edited;

    private List<ReviewMediaResponse> media;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}