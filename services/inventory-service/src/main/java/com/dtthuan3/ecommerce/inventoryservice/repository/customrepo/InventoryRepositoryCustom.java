package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.entity.Inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Các thao tác SQL đặc thù trên inventory, nhất là cập nhật atomic và khóa dòng. */
public interface InventoryRepositoryCustom {

    /** Đọc theo ID cho luồng chỉ đọc, không đặt khóa database. */
    Optional<Inventory> findInventoryRowById(String id);

    /** Đọc và khóa dòng hiện tại cho đến khi transaction kết thúc. */
    Optional<Inventory> findInventoryRowByIdForUpdate(String id);

    /** Các truy vấn danh sách tra cứu, không thay đổi dữ liệu. */
    List<Inventory> findBySku(String sku);

    List<Inventory> findByWarehouse(String warehouseId);

    List<Inventory> findByLocation(String locationId);

    /** Chỉ trả các dòng có tồn khả dụng lớn hơn 0; bộ lọc đầu vào có thể bỏ trống. */
    List<Inventory> findAvailable(String sku, String warehouseId, String locationId);

    /** Tìm theo khóa nghiệp vụ duy nhất enterprise + warehouse + location + SKU. */
    Optional<Inventory> findByPosition(
            String enterpriseId,
            String warehouseId,
            String locationId,
            String sku
    );


    /** Tạo dòng mới; Optional rỗng nếu khóa nghiệp vụ đã được một request khác tạo trước. */
    Optional<Inventory> create(
            String enterpriseId,
            String warehouseId,
            String locationId,
            String sku
    );

    /** Ghi nhiều vị trí tồn bằng JDBC batch; kết quả từng câu lệnh cho biết dòng nào bị trùng. */
    int[] createBatch(List<Inventory> inventories);

    /** Đọc lại các dòng vừa batch insert để lấy timestamp do cơ sở dữ liệu tạo. */
    List<Inventory> findByIds(List<String> ids);

    /** Tạo vị trí tồn nếu chưa có; nếu đã tồn tại thì đọc và khóa dòng để nhập an toàn. */
    Inventory createOrFind(
            String enterpriseId,
            String warehouseId,
            String locationId,
            String sku
    );

    /** Cộng hàng nhập, cập nhật thời gian sửa và tăng version bằng một câu UPDATE. */
    Inventory receive(
            String inventoryId,
            BigDecimal quantity
    );

    /** Chỉ giữ hàng khi tồn khả dụng trong database đủ tại thời điểm cập nhật. */
    Optional<Inventory> reserve(
            String inventoryId,
            BigDecimal quantity
    );
    /** Chỉ nhả hàng khi lượng đã giữ đủ đáp ứng yêu cầu. */
    Optional<Inventory> release(
            String inventoryId,
            BigDecimal quantity
    );

    /** Chỉ xuất hàng từ tồn khả dụng, ngăn tồn khả dụng xuống dưới 0. */
    Optional<Inventory> issue(
            String inventoryId,
            BigDecimal quantity
    );

    /** Chỉ điều chỉnh nếu lượng kiểm kê mới vẫn bao phủ các lượng đã cam kết. */
    Optional<Inventory> adjust(
            String inventoryId,
            BigDecimal actualQuantity
    );
}
