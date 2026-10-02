package com.dtthuan3.ecommerce.productservice.domain;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "attribute_options",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attribute_option_value",
                        columnNames = {"attribute_id", "value"}
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "attribute_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_attribute_option_attribute"
            )
    )
    private Attribute attribute;

    @Column(nullable = false, length = 150)
    private String value;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
