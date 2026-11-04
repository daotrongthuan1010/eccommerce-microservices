package com.dtthuan3.ecommerce.inventoryservice.repository;

import com.dtthuan3.ecommerce.inventoryservice.repository.customrepo.StockMovementRepositoryCustom;

/**
 * Điểm truy cập repository cho lịch sử biến động; toàn bộ truy vấn/ghi JDBC nằm trong custom repository.
 * Interface này là ranh giới truy cập dữ liệu cho service, gom các thao tác đọc/ghi cần thiết. Cách thực
 * thi bằng JPA hay JDBC nằm trong repository/adapter, không đặt trong controller.
 */
public interface StockMovementRepository extends StockMovementRepositoryCustom {
}
