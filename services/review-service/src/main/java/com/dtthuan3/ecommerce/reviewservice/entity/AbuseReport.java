package com.dtthuan3.ecommerce.reviewservice.entity;

import com.dtthuan3.ecommerce.reviewservice.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "abuse_report",
        uniqueConstraints = @UniqueConstraint(name = "uk_report_review_reporter", columnNames = {"review_id", "reporter_id"}),
        indexes = {
                @Index(name = "ix_report_status_created", columnList = "status, created_at"),
                @Index(name = "ix_report_review", columnList = "review_id, status")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbuseReport {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String id;

    @Column(name = "review_id", columnDefinition = "CHAR(36)", nullable = false)
    private String reviewId;
    @Column(name = "reporter_id", columnDefinition = "CHAR(36)", nullable = false)
    private String reporterId;
    @Column(name = "reason_code", length = 50, nullable = false)
    private String reasonCode;
    @Column(length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private ReportStatus status;
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "handled_by", columnDefinition = "CHAR(36)")
    private String handledBy;
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}