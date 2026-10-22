
package com.dtthuan3.ecommerce.productservice.domain;

import com.dtthuan3.ecommerce.productservice.constant.MediaType;
import com.dtthuan3.ecommerce.productservice.mapper.GeneratedUuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "sku_media",
        indexes = {
                @Index(name = "idx_sku_media_sku_id", columnList = "sku_id"),
                @Index(name = "idx_sku_media_revision_id", columnList = "revision_id"),
                @Index(name = "idx_sku_media_file_key", columnList = "file_key")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkuMedia extends BaseEntity {

    @jakarta.persistence.Id
    @GeneratedUuidV7
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(name = "revision_id", nullable = false)
    private UUID revisionId;

    @Column(name = "file_key", nullable = false, length = 1024)
    private String fileKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 30)
    private MediaType mediaType;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private Boolean primary = false;
}
