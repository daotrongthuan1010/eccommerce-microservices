package com.dtthuan3.ecommerce.productservice.domain;

import com.dtthuan3.ecommerce.productservice.mapper.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

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
//Giá trị thuộc tính biến thể
    @Id
    @GeneratedUuidV7
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * Variant chứa attribute value này.
     */
    @Column(
            name = "variant_id",
            nullable = false
    )
    private UUID variantId;

    @Column(
            name = "attribute_id",
            nullable = false
    )
    private UUID attributeId;

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
     * Dùng khi attribute có kiểu SELECT.
     */
    @Column(name = "option_id")
    private UUID optionId;
}