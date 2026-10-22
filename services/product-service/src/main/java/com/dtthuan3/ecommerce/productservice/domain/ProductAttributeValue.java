package com.dtthuan3.ecommerce.productservice.domain;

import com.dtthuan3.ecommerce.productservice.mapper.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "product_attribute_values",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_revision_attribute",
                        columnNames = {
                                "revision_id",
                                "attribute_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_product_attribute_revision",
                        columnList = "revision_id"
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
//Giá trị thuộc tính sản phẩm
    @Id
    @GeneratedUuidV7
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * Revision chứa giá trị attribute này.
     */
    @Column(
            name = "revision_id",
            nullable = false
    )
    private UUID revisionId;

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
     * Dùng khi AttributeDataType = SELECT.
     */
    @Column(name = "option_id")
    private UUID optionId;
}