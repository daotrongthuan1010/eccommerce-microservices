package com.dtthuan3.ecommerce.productservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "product_attribute_values",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_attribute",
                        columnNames = {
                                "product_id",
                                "attribute_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_product_attribute_product",
                        columnList = "product_id"
                ),
                @Index(
                        name = "idx_product_attribute_attribute",
                        columnList = "attribute_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttributeValue extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_product_attribute_product"
            )
    )
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "attribute_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_product_attribute_attribute"
            )
    )
    private Attribute attribute;

    /**
     * TEXT
     */
    @Column(
            name = "value_text",
            columnDefinition = "TEXT"
    )
    private String valueText;

    /**
     * NUMBER
     */
    @Column(
            name = "value_number",
            precision = 20,
            scale = 6
    )
    private BigDecimal valueNumber;

    /**
     * DATE
     */
    @Column(name = "value_date")
    private LocalDate valueDate;

    /**
     * BOOLEAN
     */
    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    /**
     * SELECT
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "option_id",
            foreignKey = @ForeignKey(
                    name = "fk_product_attribute_option"
            )
    )
    private AttributeOption option;
}
