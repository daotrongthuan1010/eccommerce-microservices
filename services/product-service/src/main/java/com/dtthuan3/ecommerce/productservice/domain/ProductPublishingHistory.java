package com.dtthuan3.ecommerce.productservice.domain;

import com.dtthuan3.ecommerce.productservice.constant.ProductStatus;
import com.dtthuan3.ecommerce.productservice.mapper.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "product_publishing_history",
        indexes = {
                @Index(
                        name = "idx_publishing_history_product",
                        columnList = "product_id"
                ),
                @Index(
                        name = "idx_publishing_history_revision",
                        columnList = "revision_id"
                ),
                @Index(
                        name = "idx_publishing_history_changed_at",
                        columnList = "changed_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPublishingHistory extends BaseEntity {
//Lịch sử xuất bản sản phẩm
    @Id
    @GeneratedUuidV7
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(
            name = "product_id",
            nullable = false
    )
    private UUID productId;

    /**
     * Revision liên quan đến lần thay đổi trạng thái.
     */
    @Column(
            name = "revision_id"
    )
    private UUID revisionId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "from_status",
            length = 30
    )
    private ProductStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "to_status",
            nullable = false,
            length = 30
    )
    private ProductStatus toStatus;

    @Column(length = 500)
    private String reason;

    /**
     * User/admin từ identity-service.
     */
    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(
            name = "changed_at",
            nullable = false
    )
    private LocalDateTime changedAt;
}