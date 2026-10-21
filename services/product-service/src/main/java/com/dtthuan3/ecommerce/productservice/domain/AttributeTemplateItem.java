package com.dtthuan3.ecommerce.productservice.domain;

import com.dtthuan3.ecommerce.productservice.mapper.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "attribute_template_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_template_attribute",
                        columnNames = {
                                "template_id",
                                "attribute_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_template_item_template",
                        columnList = "template_id"
                ),
                @Index(
                        name = "idx_template_item_attribute",
                        columnList = "attribute_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeTemplateItem extends BaseEntity {
//Chi tiết mẫu thuộc tính
    @Id
    @GeneratedUuidV7
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * AttributeTemplate chứa attribute này.
     */
    @Column(
            name = "template_id",
            nullable = false
    )
    private UUID templateId;

    /**
     * Attribute được sử dụng.
     */
    @Column(
            name = "attribute_id",
            nullable = false
    )
    private UUID attributeId;

    /**
     * Attribute này có tham gia tạo SKU/variant hay không.
     *
     * Ví dụ:
     *
     * Color    -> true
     * Storage  -> true
     * RAM      -> false
     * Voltage  -> false
     */
    @Column(
            name = "variant_attribute",
            nullable = false
    )
    @Builder.Default
    private Boolean variantAttribute = false;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(nullable = false)
    @Builder.Default
    private Boolean required = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}