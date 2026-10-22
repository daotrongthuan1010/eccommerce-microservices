package com.dtthuan3.ecommerce.productservice.domain;

import com.dtthuan3.ecommerce.productservice.mapper.GeneratedUuidV7;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "price_schedules",
        indexes = {
                @Index(
                        name = "idx_price_schedule_sku",
                        columnList = "sku_id"
                ),
                @Index(
                        name = "idx_price_schedule_effective",
                        columnList = "effective_from"
                ),
                @Index(
                        name = "idx_price_schedule_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceSchedule extends BaseEntity {
//Lịch giá
    @Id
    @GeneratedUuidV7
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * SKU được áp dụng giá.
     */
    @Column(
            name = "sku_id",
            nullable = false
    )
    private UUID skuId;

    /**
     * Giá bán.
     */
    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal price;

    /**
     * Currency.
     *
     * Requirement hiện tại: VND.
     */
    @Column(
            nullable = false,
            length = 3
    )
    @Builder.Default
    private String currency = "VND";

    /**
     * Thời điểm bắt đầu có hiệu lực.
     */
    @Column(
            name = "effective_from",
            nullable = false
    )
    private LocalDateTime effectiveFrom;

    /**
     * Thời điểm kết thúc.
     *
     * NULL = không có thời điểm kết thúc.
     */
    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    /**
     * PENDING
     * ACTIVE
     * EXPIRED
     * CANCELLED
     */
    @Column(
            nullable = false,
            length = 30
    )
    private String status;

    /**
     * Product revision/version liên quan.
     *
     * Dùng để event downstream biết version nào
     * tạo ra thay đổi giá.
     */
    @Column(name = "product_version")
    private Long productVersion;
}