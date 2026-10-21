package com.dtthuan3.ecommerce.reviewservice.entity;

import com.dtthuan3.ecommerce.reviewservice.enums.EligibilityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "eligibility_projection",
        uniqueConstraints = @UniqueConstraint(name = "uk_elig_line", columnNames = {"line_id"}),
        indexes = @Index(name = "ix_elig_user_product", columnList = "user_id, product_id")
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityProjection {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String id;

    @Column(name = "line_id", columnDefinition = "CHAR(36)", nullable = false) private String lineId;
    @Column(name = "user_id", columnDefinition = "CHAR(36)", nullable = false) private String userId;
    @Column(name = "product_id", columnDefinition = "CHAR(36)", nullable = false) private String productId;
    @Column(name = "order_id", columnDefinition = "CHAR(36)", nullable = false) private String orderId;
    @Column(name = "sku_id", columnDefinition = "CHAR(36)", nullable = false) private String skuId;

    @Enumerated(EnumType.STRING) @Column(length = 30, nullable = false) private EligibilityStatus status;
    @Column(name = "delivered_at", nullable = false) private LocalDateTime deliveredAt;
    @Column(name = "eligible_until", nullable = false) private LocalDateTime eligibleUntil;
    @Column(name = "source_event_id", columnDefinition = "CHAR(36)", nullable = false) private String sourceEventId;
    @Column(name = "source_version", nullable = false) private Long sourceVersion;
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
