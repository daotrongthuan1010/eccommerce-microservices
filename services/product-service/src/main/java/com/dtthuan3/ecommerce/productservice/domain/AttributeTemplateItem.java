package com.dtthuan3.ecommerce.productservice.domain;

import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "template_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_template_item_template"
            )
    )
    private AttributeTemplate template;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "attribute_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_template_item_attribute"
            )
    )
    private Attribute attribute;

    @Column(name = "display_order")
    private Integer displayOrder;

    /**
     * Có bắt buộc Product phải nhập attribute này không.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean required = false;
}
