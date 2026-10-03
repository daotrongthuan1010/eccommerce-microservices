package com.dtthuan3.ecommerce.productservice.domain;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "skus",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sku_code",
                        columnNames = "sku_code"
                ),
                @UniqueConstraint(
                        name = "uk_sku_barcode",
                        columnNames = "barcode"
                )
        },
        indexes = {
                @Index(
                        name = "idx_sku_variant",
                        columnList = "variant_id"
                ),
                @Index(
                        name = "idx_sku_barcode",
                        columnList = "barcode"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sku extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "variant_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_sku_variant"
            )
    )
    private ProductVariant variant;

    @Column(
            name = "sku_code",
            nullable = false,
            length = 100
    )
    private String skuCode;

    @Column(
            nullable = false,
            length = 100
    )
    private String barcode;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
