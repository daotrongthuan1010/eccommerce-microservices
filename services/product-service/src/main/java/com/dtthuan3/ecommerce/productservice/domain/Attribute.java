package com.dtthuan3.ecommerce.productservice.domain;

import com.dtthuan3.ecommerce.productservice.constant.AttributeDataType;
import com.dtthuan3.ecommerce.productservice.mapper.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "attributes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attribute_code",
                        columnNames = "code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_attribute_name",
                        columnList = "name"
                ),
                @Index(
                        name = "idx_attribute_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attribute extends BaseEntity {
//Thuộc tính
    @Id
    @GeneratedUuidV7
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * Tên hiển thị.
     *
     * Ví dụ:
     * RAM
     * Color
     * Voltage
     */
    @Column(
            nullable = false,
            length = 150
    )
    private String name;

    /**
     * Mã ổn định của attribute.
     *
     * Ví dụ:
     * ram
     * color
     * voltage
     */
    @Column(
            nullable = false,
            length = 100
    )
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "data_type",
            nullable = false,
            length = 30
    )
    private AttributeDataType dataType;

    /**
     * Đơn vị đo.
     *
     * Ví dụ:
     * GB
     * V
     * MHz
     * Ohm
     */
    @Column(length = 50)
    private String unit;

    /**
     * Required mặc định của attribute.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean required = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}