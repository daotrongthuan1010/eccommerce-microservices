package com.dtthuan3.ecommerce.inventoryservice.repository;

import com.dtthuan3.ecommerce.inventoryservice.repository.model.WarehouseRecord;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Cổng dữ liệu Warehouse để service không phụ thuộc JdbcTemplate.
 * Adapter JDBC thực thi truy vấn; entity Warehouse chỉ được Hibernate dùng để sinh/cập nhật bảng.
 */
public interface WarehouseRepository {
    /** Tìm kho theo khóa chính để đọc hoặc kiểm tra trước khi cập nhật. */
    Optional<WarehouseRecord> findById(String warehouseId);

    /** Đọc và khóa chia sẻ dòng kho trong transaction trước thao tác ghi tồn. */
    Optional<WarehouseRecord> findByIdForShare(String warehouseId);

    /** Đọc danh sách nhỏ có giới hạn để tránh tải toàn bộ kho vào bộ nhớ. */
    List<WarehouseRecord> findByEnterpriseId(String enterpriseId, int limit);

    /** Đọc một trang kho; offset/limit được ràng buộc ở lớp service/controller. */
    List<WarehouseRecord> findPageByEnterpriseId(String enterpriseId, int limit, long offset);

    /** Đếm số kho của một enterprise để tạo metadata phân trang. */
    long countByEnterpriseId(String enterpriseId);

    /** Keyset pagination theo thứ tự code/id, không phụ thuộc offset lớn hoặc COUNT(*). */
    List<WarehouseRecord> findAfterByEnterpriseId(String enterpriseId, String afterCode, String afterId, int limit);

    /** Tạo bản ghi kho; unique constraint của database chặn trùng ID hoặc mã trong enterprise. */
    void insert(WarehouseRecord warehouse);

    /** Chỉ đổi thông tin hiển thị; không cho sửa enterprise sở hữu hoặc trạng thái tại đây. */
    void updateDetails(String warehouseId, String code, String name, String address, Instant updatedAt);

    /** Đổi trạng thái mềm để giữ nguyên các tham chiếu lịch sử từ tồn kho và phiếu nhập. */
    void updateStatus(String warehouseId, String status, Instant updatedAt);
}
