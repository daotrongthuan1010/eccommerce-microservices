package com.dtthuan3.ecommerce.inventoryservice.repository;

import com.dtthuan3.ecommerce.inventoryservice.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Cổng dữ liệu Inventory; service chỉ phụ thuộc interface, không phụ thuộc JPA hay JDBC. */
public interface InventoryRepository {

    /** Đọc theo ID cho luồng chỉ đọc, không đặt khóa database. */
    Optional<Inventory> findInventoryRowById(String id);

    /** Đọc và khóa dòng hiện tại cho đến khi transaction kết thúc. */
    Optional<Inventory> findInventoryRowByIdForUpdate(String id);

    /** Các truy vấn danh sách tra cứu, không thay đổi dữ liệu. */
    Page<Inventory> findBySku(String sku, Pageable pageable);
    Page<Inventory> findByWarehouse(String warehouseId, Pageable pageable);
    Page<Inventory> findByLocation(String locationId, Pageable pageable);
    Page<Inventory> findAvailable(String sku, String warehouseId, String locationId, Pageable pageable);

    /** Tìm theo khóa nghiệp vụ duy nhất enterprise + warehouse + location + SKU. */
    Optional<Inventory> findByPosition(String enterpriseId, String warehouseId, String locationId, String sku);

    /** Lấy và khóa một lô SKU tại cùng vị trí; adapter khóa theo ID ổn định để giảm deadlock. */
    List<Inventory> findByPositionForUpdate(String enterpriseId, String warehouseId,
                                            String locationId, List<String> skus);

    /** Tạo dòng mới; Optional rỗng nếu khóa nghiệp vụ đã được một request khác tạo trước. */
    Optional<Inventory> create(String enterpriseId, String warehouseId, String locationId, String sku);

    /** Ghi nhiều vị trí tồn bằng JDBC batch; kết quả từng câu lệnh cho biết dòng nào bị trùng. */
    int[] createBatch(List<Inventory> inventories);

    /** Đọc lại các dòng vừa batch insert để lấy timestamp do cơ sở dữ liệu tạo. */
    List<Inventory> findByIds(List<String> ids);

    /** Đọc và khóa nhiều dòng theo ID tăng dần để giao dịch hai chiều tránh khóa chéo. */
    List<Inventory> findByIdsForUpdate(List<String> ids);

    /** Tạo dòng tồn nếu thiếu mà chưa khóa dòng đã tồn tại; sau đó khóa tập ID theo thứ tự chung. */
    void ensureExists(String enterpriseId, String warehouseId, String locationId, String sku);

    /** Tạo vị trí tồn nếu chưa có; nếu đã tồn tại thì đọc và khóa dòng để nhập an toàn. */
    Inventory createOrFind(String enterpriseId, String warehouseId, String locationId, String sku);

    /** Cộng hàng nhập nguyên tử; SQL chỉ nhận số lượng dương và tăng version cùng lúc. */
    Inventory receive(String inventoryId, BigDecimal quantity);

    /** Giảm lượng đang vận chuyển nếu bucket nguồn còn đủ. */
    Optional<Inventory> decreaseInTransit(String inventoryId, BigDecimal quantity);

    /** Cập nhật các bucket trong một UPDATE; SQL yêu cầu từng lượng không âm và tổng lượng lớn hơn 0. */
    Inventory receiveClassified(String inventoryId, BigDecimal goodQuantity,
                                BigDecimal damagedQuantity, BigDecimal inspectionQuantity);

    /** Chỉ chuyển bucket khi pending đủ, lượng kiểm tra dương và lượng hỏng nằm trong lượng kiểm tra. */
    Optional<Inventory> inspectPendingReturn(String inventoryId, BigDecimal inspectedQuantity,
                                             BigDecimal damagedQuantity);

    /** Chỉ giữ hàng khi tồn khả dụng trong database đủ tại thời điểm cập nhật. */
    Optional<Inventory> reserve(String inventoryId, BigDecimal quantity);

    /** Chỉ nhả hàng khi lượng đã giữ đủ đáp ứng yêu cầu. */
    Optional<Inventory> release(String inventoryId, BigDecimal quantity);

    /** Chỉ xuất hàng từ tồn khả dụng, ngăn tồn khả dụng xuống dưới 0. */
    Optional<Inventory> issue(String inventoryId, BigDecimal quantity);

    /** Dispatch phần đã reserve: giảm vật lý và reserved cùng lúc, không chạm lượng khả dụng khác. */
    Optional<Inventory> dispatchReserved(String inventoryId, BigDecimal quantity);

    /** Trừ/cộng theo lô khi từng lượng dương và tồn khả dụng tại DB vẫn đủ. */
    List<Inventory> dispatchTransferBatch(List<TransferDispatchUpdate> updates);

    /** Chỉ nhận lượng kiểm kê không âm và vẫn bao phủ các lượng đã cam kết. */
    Optional<Inventory> adjust(String inventoryId, BigDecimal actualQuantity);

    /** Ghi đồng thời ba bucket đã kiểm đếm, chỉ khi balance vẫn ở version snapshot và reserved còn hợp lệ. */
    Optional<Inventory> adjustCountedBuckets(String inventoryId, BigDecimal goodQuantity,
                                             BigDecimal defectiveQuantity, BigDecimal pendingQuantity,
                                             long expectedVersion);

    /** ID tồn nguồn và lượng cần chuyển cho một dòng đã được khóa trước khi cập nhật. */
    record TransferDispatchUpdate(String inventoryId, BigDecimal quantity) {}
}
