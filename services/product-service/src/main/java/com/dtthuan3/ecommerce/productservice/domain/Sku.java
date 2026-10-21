package com.dtthuan3.ecommerce.productservice.domain;

import com.dtthuan3.ecommerce.productservice.constant.SkuSellingStatus;
import com.dtthuan3.ecommerce.productservice.mapper.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

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
                        name = "idx_sku_status",
                        columnList = "selling_status"
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

    @Id
    @GeneratedUuidV7
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * Variant mà SKU thuộc về.
     */
    @Column(
            name = "variant_id",
            nullable = false
    )
    private UUID variantId;

    /**
     * Mã SKU nghiệp vụ.
     *
     * Ví dụ:
     * IP17-BLK-256
     */
    @Column(
            name = "sku_code",
            nullable = false,
            length = 100
    )
    private String skuCode;

    /**
     * Barcode của SKU.
     */
    @Column(
            nullable = false,
            length = 100
    )
    private String barcode;

    /**
     * Trạng thái bán hàng.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "selling_status",
            nullable = false,
            length = 30
    )
    @Builder.Default
    private SkuSellingStatus sellingStatus = SkuSellingStatus.SELLABLE;
}