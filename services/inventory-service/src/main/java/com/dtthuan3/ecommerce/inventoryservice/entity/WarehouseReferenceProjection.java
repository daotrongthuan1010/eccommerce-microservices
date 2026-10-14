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

/** Bản chiếu trạng thái và enterprise của Warehouse; warehouse master data vẫn do Warehouse Service sở hữu. */
@Entity
@Table(name = "inventory_warehouse_projection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseReferenceProjection {

    /** ID Warehouse làm khóa projection; không phải foreign key sang Warehouse Service. */
    @Id
    @Column(name = "warehouse_id", nullable = false, length = 100)
    private String warehouseId;

    /** Enterprise sở hữu Warehouse theo dữ liệu nguồn. */
    @Column(name = "enterprise_id", nullable = false, length = 100)
    private String enterpriseId;

    /** Trạng thái nguồn, hiện chỉ nhận ACTIVE hoặc INACTIVE. */
    @Column(nullable = false, length = 16)
    private String status;

    /** Version cuối nhận từ Warehouse Service để loại event cũ hoặc gửi lặp. */
    @Column(name = "source_version")
    private Long sourceVersion;

    /** Thời điểm cập nhật nguồn cuối cùng, đồng thời làm mốc dự phòng khi event thiếu version. */
    @Column(name = "source_updated_at", nullable = false)
    private Instant sourceUpdatedAt;
}
