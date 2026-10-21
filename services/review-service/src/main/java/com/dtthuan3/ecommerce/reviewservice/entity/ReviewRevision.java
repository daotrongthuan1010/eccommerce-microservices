package com.dtthuan3.ecommerce.reviewservice.entity;

import com.dtthuan3.ecommerce.reviewservice.enums.RevisionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_revision",
        uniqueConstraints = @UniqueConstraint(name = "uk_revision_review_no", columnNames = {"review_id", "revision_no"}),
        indexes = @Index(name = "ix_revision_status_submitted", columnList = "status, submitted_at")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRevision {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String id;

    @Column(name = "review_id", columnDefinition = "CHAR(36)", nullable = false)
    private String reviewId;
    @Column(name = "revision_no", nullable = false)
    private Integer revisionNo;
    @Column(nullable = false)
    private Integer rating;
    @Column(length = 3000, nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private RevisionStatus status;
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;
}