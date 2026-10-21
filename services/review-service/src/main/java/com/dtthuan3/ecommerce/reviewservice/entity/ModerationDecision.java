package com.dtthuan3.ecommerce.reviewservice.entity;

import com.dtthuan3.ecommerce.reviewservice.enums.ModerationAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "moderation_decision", indexes = {
        @Index(name = "ix_decision_review", columnList = "review_id, decided_at DESC"),
        @Index(name = "ix_decision_type_time", columnList = "decision, decided_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationDecision {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String id;

    @Column(name = "review_id", columnDefinition = "CHAR(36)", nullable = false)
    private String reviewId;
    @Column(name = "revision_id", columnDefinition = "CHAR(36)", nullable = false)
    private String revisionId;
    @Column(name = "abuse_report_id", columnDefinition = "CHAR(36)")
    private String abuseReportId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private ModerationAction decision;
    @Column(name = "reason_code", length = 50, nullable = false)
    private String reasonCode;
    @Column(name = "reason_note", length = 1000)
    private String reasonNote;
    @Column(name = "decided_by", columnDefinition = "CHAR(36)", nullable = false)
    private String decidedBy;
    @Column(name = "decided_at", nullable = false)
    private LocalDateTime decidedAt;
}
