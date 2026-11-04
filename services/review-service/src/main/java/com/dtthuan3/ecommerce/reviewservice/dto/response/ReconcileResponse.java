package com.dtthuan3.ecommerce.reviewservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconcileResponse {
    private String productId;
    /** true nếu rating_aggregate khớp tập review APPROVED. */
    private boolean consistent;
    /** true nếu đã sửa lại aggregate theo tập APPROVED. */
    private boolean repaired;
    private long expectedTotalStars;
    private int expectedReviewCount;
    private long actualTotalStars;
    private int actualReviewCount;
}
