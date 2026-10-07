package com.dtthuan3.ecommerce.reviewservice.entity;

import com.dtthuan3.ecommerce.reviewservice.entity.enums.ReviewStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_user_order_item",
                        columnNames = {
                                "user_id",
                                "order_item_id"
                        }
                )
        },

        indexes = {
                @Index(
                        name = "idx_review_product_id",
                        columnList = "product_id"
                ),

                @Index(
                        name = "idx_review_user_id",
                        columnList = "user_id"
                ),

                @Index(
                        name = "idx_review_order_id",
                        columnList = "order_id"
                ),

                @Index(
                        name = "idx_review_order_item_id",
                        columnList = "order_item_id"
                ),

                @Index(
                        name = "idx_review_rating",
                        columnList = "rating"
                ),

                @Index(
                        name = "idx_review_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /*
     * =========================================
     * USER
     * =========================================
     *
     * ID user trong user-service.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;


    /*
     * =========================================
     * ORDER
     * =========================================
     */
    @Column(name = "order_id", nullable = false)
    private Long orderId;


    /*
     * Quan trọng hơn orderId + productId.
     *
     * Một Order:
     *
     * order
     * ├── iPhone 17 - Black
     * ├── iPhone 17 - White
     * └── AirPods
     *
     * mỗi OrderItem có thể review riêng.
     */
    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;


    /*
     * =========================================
     * PRODUCT
     * =========================================
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;


    /*
     * Variant nếu sản phẩm có phân loại.
     *
     * Ví dụ:
     *
     * iPhone 17
     *
     * variant:
     * - Black / 256GB
     * - White / 512GB
     *
     * Có thể null nếu sản phẩm không có variant.
     */
    @Column(name = "variant_id")
    private Long variantId;


    /*
     * =========================================
     * RATING
     * =========================================
     *
     * 1 -> 5 sao.
     */
    @Column(name = "rating", nullable = false)
    private Integer rating;


    /*
     * =========================================
     * REVIEW CONTENT
     * =========================================
     */
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;


    /*
     * =========================================
     * VERIFIED PURCHASE
     * =========================================
     *
     * true khi review-service xác nhận:
     *
     * user
     *     ↓
     * thực sự sở hữu order
     *     ↓
     * order có orderItem
     *     ↓
     * order đã DELIVERED / COMPLETED
     */
    @Column(name = "verified_purchase", nullable = false)
    @Builder.Default
    private boolean verifiedPurchase = false;


    /*
     * =========================================
     * ANONYMOUS REVIEW
     * =========================================
     *
     * Tương tự tính năng đánh giá ẩn danh.
     */
    @Column(name = "anonymous", nullable = false)
    @Builder.Default
    private boolean anonymous = false;


    /*
     * =========================================
     * HELPFUL
     * =========================================
     *
     * Bao nhiêu người thấy review hữu ích.
     *
     * Ví dụ:
     *
     * 👍 Hữu ích (25)
     */
    @Column(name = "helpful_count", nullable = false)
    @Builder.Default
    private Long helpfulCount = 0L;


    /*
     * =========================================
     * SELLER REPLY
     * =========================================
     *
     * Shop có thể phản hồi đánh giá.
     */
    @Column(name = "seller_reply", columnDefinition = "TEXT")
    private String sellerReply;


    @Column(name = "seller_replied_at")
    private LocalDateTime sellerRepliedAt;


    /*
     * =========================================
     * STATUS
     * =========================================
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PUBLISHED;


    /*
     * =========================================
     * EDIT
     * =========================================
     *
     * Có thể dùng để xác định review đã sửa chưa.
     */
    @Column(name = "edited", nullable = false)
    @Builder.Default
    private boolean edited = false;


    /*
     * =========================================
     * CREATED TIME
     * =========================================
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    /*
     * =========================================
     * UPDATED TIME
     * =========================================
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    /*
     * =========================================
     * JPA CALLBACK
     * =========================================
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ReviewStatus.PUBLISHED;
        }
    }


    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}