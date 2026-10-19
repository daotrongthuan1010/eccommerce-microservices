package com.dtthuan3.ecommerce.productservice.domain;

import com.dtthuan3.ecommerce.productservice.mapper.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "publication_approvals",
        indexes = {
                @Index(
                        name = "idx_approval_revision",
                        columnList = "revision_id"
                ),
                @Index(
                        name = "idx_approval_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_approval_submitted_by",
                        columnList = "submitted_by"
                ),
                @Index(
                        name = "idx_approval_approved_by",
                        columnList = "approved_by"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicationApproval extends BaseEntity {
//Phê duyệt xuất bản
    @Id
    @GeneratedUuidV7
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * Revision cần được approve.
     */
    @Column(
            name = "revision_id",
            nullable = false
    )
    private UUID revisionId;

    /**
     * User/admin submit revision.
     */
    @Column(
            name = "submitted_by",
            nullable = false
    )
    private UUID submittedBy;

    /**
     * User/admin approve/reject.
     *
     * Có thể NULL khi chưa xử lý.
     */
    @Column(name = "approved_by")
    private UUID approvedBy;

    /**
     * PENDING
     * APPROVED
     * REJECTED
     */
    @Column(
            nullable = false,
            length = 30
    )
    private String status;

    /**
     * Lý do reject hoặc ghi chú approval.
     */
    @Column(
            length = 1000
    )
    private String reason;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}