
package com.dtthuan3.ecommerce.reviewservice.entity;

import com.dtthuan3.ecommerce.reviewservice.enums.ReviewMediaType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "review_media",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_review_media_revision_file",
                        columnNames = {"revision_id", "file_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_review_media_review",
                        columnList = "review_id"
                ),
                @Index(
                        name = "idx_review_media_revision",
                        columnList = "revision_id"
                ),
                @Index(
                        name = "idx_review_media_file",
                        columnList = "file_id"
                ),
                @Index(
                        name = "idx_review_media_review_type",
                        columnList = "review_id, media_type"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "id",
            columnDefinition = "CHAR(36)",
            nullable = false,
            updatable = false
    )
    private String id;

    @Column(
            name = "review_id",
            columnDefinition = "CHAR(36)",
            nullable = false
    )
    private String reviewId;

    @Column(
            name = "revision_id",
            columnDefinition = "CHAR(36)"
    )
    private String revisionId;

    @Column(
            name = "file_id",
            columnDefinition = "CHAR(36)",
            nullable = false
    )
    private String fileId;

    @Column(
            name = "binding_id",
            columnDefinition = "CHAR(36)"
    )
    private String bindingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private ReviewMediaType mediaType;

    @Column(name = "media_url", length = 1000)
    private String mediaUrl;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
}
