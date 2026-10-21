package com.dtthuan3.ecommerce.reviewservice.entity;

import com.dtthuan3.ecommerce.reviewservice.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Entity
@Table(name = "review", indexes = {
        @Index(name = "ix_review_user", columnList = "user_id, submitted_at DESC"),
        @Index(name = "ix_review_product_public", columnList = "product_id, status, published_at DESC")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String id;

    @Column(name = "line_id", columnDefinition = "CHAR(36)", nullable = false, unique = true)
    private String lineId;

    @Column(name = "order_id", columnDefinition = "CHAR(36)", nullable = false)
    private String orderId;

    @Column(name = "user_id", columnDefinition = "CHAR(36)", nullable = false)
    private String userId;

    @Column(name = "product_id", columnDefinition = "CHAR(36)", nullable = false)
    private String productId;

    @Column(name = "sku_id", columnDefinition = "CHAR(36)")
    private String skuId;

    @Column(name = "reviewer_display_name", length = 100, nullable = false)
    private String reviewerDisplayName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private ReviewStatus status;

    @Column(name = "rating")
    private Integer rating;
    @Column(name = "has_photo", nullable = false)
    private Boolean hasPhoto;

    @Column(name = "published_revision_id", columnDefinition = "CHAR(36)")
    private String publishedRevisionId;

    @Column(name = "latest_revision_no", nullable = false)
    private Integer latestRevisionNo;
    @Column(name = "helpful_count", nullable = false)
    private Integer helpfulCount;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
    @Column(name = "editable_until", nullable = false)
    private LocalDateTime editableUntil;

    @Version
    @Column(nullable = false)
    private Long version;
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
