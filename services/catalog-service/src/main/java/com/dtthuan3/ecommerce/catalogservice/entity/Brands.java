package com.dtthuan3.ecommerce.catalogservice.entity;

import com.dtthuan3.ecommerce.catalogservice.contstant.BrandsStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "brands",
        indexes = {
                @Index(
                        name = "idx_brands_slug",
                        columnList = "slug"
                ),
                @Index(
                        name = "idx_brands_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brands extends BaseEntity {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "brands_seq"
    )
    @SequenceGenerator(
            name = "brands_seq",
            sequenceName = "brands_id_seq",
            allocationSize = 50
    )
    private Long id;

    /**
     * Tên thương hiệu
     */
    @Column(
            name = "name",
            nullable = false,
            length = 255
    )
    private String name;

    /**
     * Slug thương hiệu
     */
    @Column(
            name = "slug",
            nullable = false,
            unique = true,
            length = 255
    )
    private String slug;

    /**
     * Mô tả thương hiệu
     */
    @Column(
            name = "description",
            columnDefinition = "TEXT"
    )
    private String description;

    /**
     * Object key của logo trong MinIO.
     *
     * Ví dụ:
     *
     * brands/2026/10/04/550e8400-e29b-41d4-a716-446655440000.png
     */
    @Column(
            name = "logo_object_key",
            length = 500
    )
    private String logoObjectKey;

    /**
     * Trạng thái thương hiệu
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    @Builder.Default
    private BrandsStatus status = BrandsStatus.ACTIVE;
}