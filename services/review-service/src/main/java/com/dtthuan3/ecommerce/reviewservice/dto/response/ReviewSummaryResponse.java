package com.dtthuan3.ecommerce.reviewservice.dto.response;

import lombok.*;

import java.util.Map;

/** Điểm tổng hợp lấy từ read model rating_aggregate (O(1)) + số lượng theo bộ lọc. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSummaryResponse {
    private String productId;
    private long totalReviews;
    /** total_stars / review_count, làm tròn 1 chữ số khi hiển thị. */
    private double averageRating;
    /** Giữ nguyên tổng sao để đối chiếu. */
    private long totalStars;
    /** Khóa 5..1 -> số đánh giá. */
    private Map<Integer, Long> distribution;
    /** Số đánh giá có ảnh. */
    private long withMedia;
    /** Số đánh giá có nội dung chữ. */
    private long withComment;
    /** Catalog dùng làm sourceVersion để bỏ dữ liệu cũ. */
    private long aggregateVersion;
}
