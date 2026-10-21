package com.dtthuan3.ecommerce.reviewservice.entity;

import com.dtthuan3.ecommerce.reviewservice.enums.IdempotencyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_record",
        uniqueConstraints = @UniqueConstraint(name = "uk_idem_actor_op_key", columnNames = {"actor_id", "operation_type", "idempotency_key"}),
        indexes = @Index(name = "ix_idem_expires", columnList = "expires_at")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String id;

    @Column(name = "actor_id", columnDefinition = "CHAR(36)", nullable = false)
    private String actorId;
    @Column(name = "operation_type", length = 40, nullable = false)
    private String operationType;
    @Column(name = "idempotency_key", length = 100, nullable = false)
    private String idempotencyKey;
    @Column(length = 64, nullable = false)
    private String fingerprint;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private IdempotencyStatus status;
    @Column(name = "result_ref", columnDefinition = "CHAR(36)")
    private String resultRef;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}