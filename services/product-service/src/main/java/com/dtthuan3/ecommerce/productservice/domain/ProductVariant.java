package com.dtthuan3.ecommerce.productservice.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "product_variants",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_variant_code",
                        columnNames = "variant_code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_variant_product",
                        columnList = "product_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;


    @Column(
            name = "product_id",
            nullable = false
    )
    private Long productId;

    @Column(
            name = "variant_code",
            nullable = false,
            length = 100
    )
    private String variantCode;

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