package com.dtthuan3.ecommerce.productservice.domain;

import com.dtthuan3.ecommerce.productservice.mapper.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "attribute_templates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attribute_template_code",
                        columnNames = "code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_attribute_template_name",
                        columnList = "name"
                ),
                @Index(
                        name = "idx_attribute_template_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeTemplate extends BaseEntity {
//Mẫu thuộc tính
    @Id
    @GeneratedUuidV7
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(
            nullable = false,
            length = 150
    )
    private String name;

    @Column(
            nullable = false,
            length = 100
    )
    private String code;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}