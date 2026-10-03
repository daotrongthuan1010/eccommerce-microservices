package com.dtthuan3.ecommerce.productservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "variant_attribute_values",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_variant_attribute",
                        columnNames = {
                                "variant_id",
                                "attribute_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_variant_attribute_variant",
                        columnList = "variant_id"
                ),
                @Index(
                        name = "idx_variant_attribute_attribute",
                        columnList = "attribute_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantAttributeValue extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "variant_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_variant_attribute_variant"
            )
    )
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "attribute_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_variant_attribute_attribute"
            )
    )
    private Attribute attribute;

    @Column(
            name = "value_text",
            columnDefinition = "TEXT"
    )
    private String valueText;

    @Column(
            name = "value_number",
            precision = 20,
            scale = 6
    )
    private BigDecimal valueNumber;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "option_id",
            foreignKey = @ForeignKey(
                    name = "fk_variant_attribute_option"
            )
    )
    private AttributeOption option;
}
