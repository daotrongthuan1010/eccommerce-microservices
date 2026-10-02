package com.dtthuan3.ecommerce.productservice.domain;


import com.dtthuan3.ecommerce.productservice.constant.MediaType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "product_media",
        indexes = {
                @Index(
                        name = "idx_product_media_product",
                        columnList = "product_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductMedia extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_product_media_product"
            )
    )
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "media_type",
            nullable = false,
            length = 30
    )
    private MediaType mediaType;

    /**
     * Object key trong MinIO.
     *
     * Ví dụ:
     * products/100/datasheet/esp32.pdf
     */
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

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean primary = false;
}
