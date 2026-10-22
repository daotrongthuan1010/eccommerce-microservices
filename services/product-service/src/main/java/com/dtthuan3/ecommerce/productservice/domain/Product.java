package com.dtthuan3.ecommerce.productservice.domain;

import com.dtthuan3.ecommerce.productservice.constant.ProductStatus;
import com.dtthuan3.ecommerce.productservice.mapper.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

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
                ),
                @Index(
                        name = "idx_product_template",
                        columnList = "attribute_template_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Id
    @GeneratedUuidV7
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * Mã nghiệp vụ của Product.
     * Ví dụ: IPHONE-17.
     */
    @Column(
            name = "product_code",
            nullable = false,
            length = 100
    )
    private String productCode;

    /**
     * ID Category từ catalog-service.
     */
    @Column(
            name = "category_id",
            nullable = false
    )
    private UUID categoryId;

    /**
     * ID Brand từ catalog-service.
     */
    @Column(name = "brand_id")
    private UUID brandId;

    /**
     * ID Manufacturer từ catalog-service.
     */
    @Column(name = "manufacturer_id")
    private UUID manufacturerId;

    /**
     * AttributeTemplate sử dụng cho Product.
     */
    @Column(name = "attribute_template_id")
    private UUID attributeTemplateId;

    /**
     * Revision hiện tại đang được chỉnh sửa.
     */
    @Column(name = "current_revision_id")
    private UUID currentRevisionId;

    /**
     * Revision hiện đang được khách hàng nhìn thấy/bán.
     */
    @Column(name = "published_revision_id")
    private UUID publishedRevisionId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;
}