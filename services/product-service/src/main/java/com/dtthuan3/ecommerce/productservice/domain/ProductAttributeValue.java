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
                ),
                @Index(
                        name = "idx_product_attribute_option",
                        columnList = "option_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttributeValue extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;


    @Column(
            name = "product_id",
            nullable = false
    )
    private Long productId;

    @Column(
            name = "attribute_id",
            nullable = false
    )
    private Long attributeId;

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
     *
     * ID của AttributeOption.
     */
    @Column(name = "option_id")
    private Long optionId;
}