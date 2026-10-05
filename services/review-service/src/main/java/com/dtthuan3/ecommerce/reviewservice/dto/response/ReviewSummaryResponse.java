package com.dtthuan3.ecommerce.reviewservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSummaryResponse {

    private Long productId;

    private long totalReviews;

    private double averageRating;

    private long oneStar;

    private long twoStars;

    private long threeStars;

    private long fourStars;

    private long fiveStars;

    private long withImages;

    private long withVideos;
}