package com.dtthuan3.ecommerce.productservice.domain;


import com.dtthuan3.ecommerce.productservice.constant.ProductStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "product_publishing_history",
        indexes = {
                @Index(
                        name = "idx_publishing_history_product",
                        columnList = "product_id"
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_publishing_history_product"
            )
    )
    private Product product;

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
     * ID của user/admin bên identity/user-service.
     *
     * Không tạo @ManyToOne User.
     */
    @Column(name = "changed_by")
    private Long changedBy;

    @Column(
            name = "changed_at",
            nullable = false
    )
    private LocalDateTime changedAt;
}
