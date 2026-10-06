package com.dtthuan3.ecommerce.productservice.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "attribute_template_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_template_attribute",
                        columnNames = {"template_id", "attribute_id"}
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

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "attribute_id", nullable = false)
    private Long attributeId;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(nullable = false)
    @Builder.Default
    private Boolean required = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}