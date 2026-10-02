package com.dtthuan3.ecommerce.productservice.domain;


import com.dtthuan3.ecommerce.productservice.constant.ProductStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_code",
                        columnNames = "product_code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_product_name",
                        columnList = "name"
                ),
                @Index(
                        name = "idx_product_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_product_category",
                        columnList = "category_id"
                ),
                @Index(
                        name = "idx_product_brand",
                        columnList = "brand_id"
                ),
                @Index(
                        name = "idx_product_manufacturer",
                        columnList = "manufacturer_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    /**
     * Mã sản phẩm gốc.
     * Không phải SKU.
     */
    @Column(
            name = "product_code",
            nullable = false,
            length = 100
    )
    private String productCode;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(
            name = "short_description",
            length = 1000
    )
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(
            name = "origin_country",
            length = 100
    )
    private String originCountry;

    /**
     * ID từ catalog-service.
     */
    @Column(
            name = "category_id",
            nullable = false
    )
    private Long categoryId;

    /**
     * ID từ catalog-service.
     */
    @Column(name = "brand_id")
    private Long brandId;

    /**
     * ID từ catalog-service.
     */
    @Column(name = "manufacturer_id")
    private Long manufacturerId;

    /**
     * Template thuộc product-service.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "attribute_template_id",
            foreignKey = @ForeignKey(
                    name = "fk_product_attribute_template"
            )
    )
    private AttributeTemplate attributeTemplate;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<ProductAttributeValue> attributeValues =
            new ArrayList<>();

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<ProductVariant> variants =
            new ArrayList<>();
}
