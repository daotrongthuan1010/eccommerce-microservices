package com.dtthuan3.ecommerce.productservice.domain;

import com.dtthuan3.ecommerce.productservice.mapper.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "product_variants",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_variant_product_code",
                        columnNames = {
                                "product_id",
                                "variant_code"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_variant_product",
                        columnList = "product_id"
                ),
                @Index(
                        name = "idx_variant_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant extends BaseEntity {
//Biến thể sản phẩm
    @Id
    @GeneratedUuidV7
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * Product/SPU sở hữu variant này.
     */
    @Column(
            name = "product_id",
            nullable = false
    )
    private UUID productId;

    /**
     * Mã variant.
     *
     * Ví dụ:
     * BLACK
     * BLUE
     */
    @Column(
            name = "variant_code",
            nullable = false,
            length = 100
    )
    private String variantCode;

    /**
     * Tên variant hiển thị.
     */
    @Column(
            nullable = false,
            length = 255
    )
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}