package com.dtthuan3.ecommerce.inventoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Biên nhận cho một parcel hàng khách trả được đưa vào kho.
 * Bản ghi phục vụ đối soát và nhận diện request retry để không cộng tồn lặp;
 * Java lưu các ID dạng chuỗi, còn khóa ngoại tới Inventory được cơ sở dữ liệu kiểm soát.
 */
@Entity
@Table(name = "inventory_return_receipt",
        uniqueConstraints = @UniqueConstraint(name = "uk_return_receipt_parcel_line",
                columnNames = {"return_request_id", "order_line_id", "parcel_identity"}),
        indexes = @Index(name = "idx_return_receipt_inventory", columnList = "inventory_id"))
@Getter
@Setter
@NoArgsConstructor
public class InventoryReturnReceipt {

    /** UUID định danh duy nhất cho lần nhận parcel. */
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    /** ID yêu cầu trả hàng từ service bên ngoài; chỉ lưu ID, không tạo quan hệ JPA. */
    @Column(name = "return_request_id", nullable = false, length = 100)
    private String returnRequestId;

    /** ID dòng hàng thuộc yêu cầu trả; kết hợp với returnRequestId để đối soát. */
    @Column(name = "order_line_id", nullable = false, length = 100)
    private String orderLineId;

    /** Mã nhận diện parcel; duy nhất trong phạm vi một dòng của yêu cầu trả để chống nhận trùng. */
    @Column(name = "parcel_identity", nullable = false, length = 100)
    private String parcelIdentity;

    /** ID dòng Inventory được tăng số lượng khi parcel được ghi nhận. */
    @Column(name = "inventory_id", nullable = false, length = 36)
    private String inventoryId;

    /** Doanh nghiệp nhận hàng trả, dùng để giữ đúng phạm vi dữ liệu kho. */
    @Column(name = "enterprise_id", nullable = false, length = 100)
    private String enterpriseId;

    /** ID kho thực tế nhận parcel. */
    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    /** ID vị trí nhận; hàng trả được đưa vào vị trí cách ly/chờ kiểm tra. */
    @Column(name = "location_id", nullable = false, length = 36)
    private String locationId;

    /** Mã SKU của hàng trong parcel. */
    @Column(nullable = false, length = 100)
    private String sku;

    /** Số lượng tối đa đã duyệt cho dòng hàng trả tại thời điểm ghi nhận. */
    @Column(name = "approved_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal approvedQuantity;

    /** Số lượng có trong parcel này; khác với tổng receivedQuantity ở tiến độ cả dòng. */
    @Column(name = "received_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal receivedQuantity;

    /** Tổng lượng đã nhận của dòng sau khi cộng parcel này; lưu để đối soát response/audit. */
    @Column(name = "line_received_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineReceivedAfter;

    /** Version của Inventory sau lần cộng lượng parcel, giúp xác định trạng thái tồn tương ứng. */
    @Column(name = "inventory_version_after", nullable = false)
    private Long inventoryVersionAfter;

    /** Định danh người dùng thực hiện thao tác nhận parcel, lấy từ ngữ cảnh xác thực. */
    @Column(name = "performed_by", length = 255)
    private String performedBy;

    /** Ghi chú lý do hoặc tình trạng được nhân viên ghi nhận khi nhận hàng trả. */
    @Column(length = 500)
    private String reason;

    /** Thời điểm parcel được ghi nhận vào kho. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
