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
                ),
                @Index(
                        name = "idx_variant_attribute_option",
                        columnList = "option_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantAttributeValue extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;


    @Column(
            name = "variant_id",
            nullable = false
    )
    private Long variantId;

    @Column(
            name = "attribute_id",
            nullable = false
    )
    private Long attributeId;

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

    /**
     * ID của AttributeOption.
     */
    @Column(name = "option_id")
    private Long optionId;
}