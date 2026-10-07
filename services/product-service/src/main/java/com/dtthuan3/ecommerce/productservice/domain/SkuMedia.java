package com.dtthuan3.ecommerce.productservice.domain;

import com.dtthuan3.ecommerce.productservice.constant.MediaType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "sku_media",
        indexes = {
                @Index(
                        name = "idx_sku_media_sku",
                        columnList = "sku_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkuMedia extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;


    @Column(
            name = "sku_id",
            nullable = false
    )
    private Long skuId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "media_type",
            nullable = false,
            length = 30
    )
    private MediaType mediaType;

    @Column(
            name = "storage_key",
            nullable = false,
            length = 500
    )
    private String storageKey;

    @Column(
            name = "file_name",
            length = 255
    )
    private String fileName;

    @Column(
            name = "content_type",
            length = 100
    )
    private String contentType;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(
            name = "is_primary",
            nullable = false
    )
    @Builder.Default
    private Boolean primary = false;
}