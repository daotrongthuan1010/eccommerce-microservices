package com.dtthuan3.ecommerce.reviewservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rating_aggregate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingAggregate {

    @Id
    @Column(name = "product_id", columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String productId;

    @Column(name = "total_stars", nullable = false)
    private Long totalStars;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;

    @Column(name = "star1_count", nullable = false)
    private Integer star1Count;
    @Column(name = "star2_count", nullable = false)
    private Integer star2Count;
    @Column(name = "star3_count", nullable = false)
    private Integer star3Count;
    @Column(name = "star4_count", nullable = false)
    private Integer star4Count;
    @Column(name = "star5_count", nullable = false)
    private Integer star5Count;

    @Column(name = "aggregate_version", nullable = false)
    private Long aggregateVersion;

    @Column(name = "last_reconciled_at")
    private LocalDateTime lastReconciledAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Cộng (+1) hoặc trừ (-1) một review có số sao {@code rating}. */
    public void apply(int rating, int sign) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("rating phải trong 1..5");
        }
        totalStars = Math.max(0L, totalStars + (long) sign * rating);
        reviewCount = Math.max(0, reviewCount + sign);
        setStar(rating, Math.max(0, star(rating) + sign));
    }

    public int star(int rating) {
        return switch (rating) {
            case 1 -> star1Count;
            case 2 -> star2Count;
            case 3 -> star3Count;
            case 4 -> star4Count;
            case 5 -> star5Count;
            default -> throw new IllegalArgumentException("rating phải trong 1..5");
        };
    }

    public void setStar(int rating, int value) {
        switch (rating) {
            case 1 -> star1Count = value;
            case 2 -> star2Count = value;
            case 3 -> star3Count = value;
            case 4 -> star4Count = value;
            case 5 -> star5Count = value;
            default -> throw new IllegalArgumentException("rating phải trong 1..5");
        }
    }

    /** Điểm trung bình giữ nguyên tỉ lệ total/count (không làm tròn); 0 nếu chưa có review. */
    public double average() {
        return reviewCount == 0 ? 0.0 : (double) totalStars / reviewCount;
    }
}
