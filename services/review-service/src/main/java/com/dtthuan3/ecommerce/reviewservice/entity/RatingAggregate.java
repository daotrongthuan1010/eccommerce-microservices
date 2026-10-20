package com.dtthuan3.ecommerce.reviewservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "rating_aggregate")
@EntityListeners(AuditingEntityListener.class)
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
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
