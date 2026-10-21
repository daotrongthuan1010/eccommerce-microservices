package com.dtthuan3.ecommerce.inventoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Bản chiếu tối thiểu của SKU dùng để kiểm tra tồn mà không sở hữu dữ liệu Catalog/Product gốc. */
@Entity
@Table(name = "inventory_sku_projection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkuCatalogProjection {

    /** SKU làm khóa projection; Inventory không trở thành nguồn dữ liệu gốc của Catalog. */
    @Id
    @Column(name = "sku_code", nullable = false, length = 100)
    private String skuCode;

    /** Trạng thái active cuối cùng đã nhận từ Product event hoặc xác thực đồng bộ. */
    @Column(nullable = false)
    private boolean active;

    /** Version cuối của Product Service; null nếu trạng thái đến từ API đối soát chưa có version. */
    @Column(name = "source_version")
    private Long sourceVersion;

    /** Mốc freshness của projection, dùng quyết định có cần gọi Product Service để đối soát hay không. */
    @Column(name = "source_updated_at", nullable = false)
    private Instant sourceUpdatedAt;
}
