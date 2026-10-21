package com.dtthuan3.ecommerce.productservice.domain;

import com.dtthuan3.ecommerce.productservice.mapper.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "import_sessions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_import_idempotency_key",
                        columnNames = "idempotency_key"
                )
        },
        indexes = {
                @Index(
                        name = "idx_import_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_import_created_by",
                        columnList = "created_by"
                ),
                @Index(
                        name = "idx_import_file",
                        columnList = "file_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportSession extends BaseEntity {

    @Id
    @GeneratedUuidV7
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * File chứa dữ liệu import.
     *
     * File-service sở hữu file.
     */
    @Column(
            name = "file_id",
            nullable = false
    )
    private UUID fileId;

    /**
     * Chống import cùng một file/request nhiều lần.
     */
    @Column(
            name = "idempotency_key",
            nullable = false,
            length = 200
    )
    private String idempotencyKey;

    /**
     * PENDING
     * PROCESSING
     * COMPLETED
     * PARTIAL_SUCCESS
     * FAILED
     */
    @Column(
            nullable = false,
            length = 30
    )
    private String status;

    @Column(
            name = "total_rows",
            nullable = false
    )
    @Builder.Default
    private Long totalRows = 0L;

    @Column(
            name = "success_rows",
            nullable = false
    )
    @Builder.Default
    private Long successRows = 0L;

    @Column(
            name = "failed_rows",
            nullable = false
    )
    @Builder.Default
    private Long failedRows = 0L;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * User/admin thực hiện import.
     */
    @Column(
            name = "created_by",
            nullable = false
    )
    private UUID createdBy;
}