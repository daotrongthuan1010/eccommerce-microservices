package com.dtthuan3.ecommerce.productservice.domain;

import com.dtthuan3.ecommerce.productservice.mapper.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "attribute_options",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attribute_option_value",
                        columnNames = {
                                "attribute_id",
                                "value"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_attribute_option_attribute",
                        columnList = "attribute_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeOption extends BaseEntity {
//Giá trị lựa chọn của thuộc tính
    @Id
    @GeneratedUuidV7
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * ID của Attribute.
     */
    @Column(
            name = "attribute_id",
            nullable = false
    )
    private UUID attributeId;

    /**
     * Giá trị lựa chọn.
     *
     * Ví dụ:
     * Black
     * Blue
     * 8GB
     * 16GB
     */
    @Column(
            nullable = false,
            length = 150
    )
    private String value;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}