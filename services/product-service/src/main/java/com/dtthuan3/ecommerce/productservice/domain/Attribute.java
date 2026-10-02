package com.dtthuan3.ecommerce.productservice.domain;

import com.dtthuan3.ecommerce.productservice.constant.AttributeDataType;
import jakarta.persistence.*;
import lombok.*;

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
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attribute extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    /**
     * Mã định danh ổn định của attribute.
     * Ví dụ:
     * ram
     * flash
     * gpio
     * voltage
     */
    @Column(nullable = false, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 30)
    private AttributeDataType dataType;

    /**
     * Đơn vị đo.
     * Ví dụ: MB, V, MHz, Ohm...
     */
    @Column(length = 50)
    private String unit;

    /**
     * Attribute có bắt buộc nhập hay không
     * ở mức mặc định.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean required = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
