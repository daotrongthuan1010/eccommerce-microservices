package com.dtthuan3.ecommerce.productservice.domain;

import com.dtthuan3.ecommerce.productservice.mapper.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "product_revisions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_revision_version",
                        columnNames = {
                                "product_id",
                                "version"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_product_revision_product",
                        columnList = "product_id"
                ),
                @Index(
                        name = "idx_product_revision_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRevision extends BaseEntity {
//Phiên bản chỉnh sửa sản phẩm
    @Id
    @GeneratedUuidV7
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * Product/SPU.
     */
    @Column(
            name = "product_id",
            nullable = false
    )
    private UUID productId;

    /**
     * Version nghiệp vụ.
     *
     * Ví dụ:
     * 1, 2, 3, 4...
     */
    @Column(
            nullable = false
    )
    private Long version;

    /**
     * DRAFT
     * PENDING_APPROVAL
     * PUBLISHED
     * REJECTED
     */
    @Column(
            nullable = false,
            length = 30
    )
    private String status;

    @Column(
            nullable = false,
            length = 255
    )
    private String name;

    @Column(
            name = "short_description",
            length = 1000
    )
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(
            name = "origin_country",
            length = 100
    )
    private String originCountry;

    @Column(
            name = "storage_condition",
            length = 500
    )
    private String storageCondition;

    @Column(
            name = "delivery_condition",
            length = 500
    )
    private String deliveryCondition;

    @Column(
            name = "return_policy",
            columnDefinition = "TEXT"
    )
    private String returnPolicy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(
            name = "rejection_reason",
            length = 1000
    )
    private String rejectionReason;
}