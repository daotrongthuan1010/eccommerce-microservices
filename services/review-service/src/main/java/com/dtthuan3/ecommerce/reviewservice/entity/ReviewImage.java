package com.dtthuan3.ecommerce.reviewservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_image",
        uniqueConstraints = @UniqueConstraint(name = "uk_image_revision_file", columnNames = {"revision_id", "file_id"}),
        indexes = @Index(name = "ix_image_file", columnList = "file_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewImage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String id;

    @Column(name = "revision_id", columnDefinition = "CHAR(36)", nullable = false)
    private String revisionId;
    @Column(name = "file_id", columnDefinition = "CHAR(36)", nullable = false)
    private String fileId;
    @Column(name = "binding_id", columnDefinition = "CHAR(36)")
    private String bindingId;
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
