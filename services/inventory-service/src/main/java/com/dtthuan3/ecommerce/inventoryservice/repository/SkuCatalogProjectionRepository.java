package com.dtthuan3.ecommerce.inventoryservice.repository;

import com.dtthuan3.ecommerce.inventoryservice.entity.SkuCatalogProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Truy vấn projection SKU cục bộ do sự kiện Catalog đồng bộ để Inventory kiểm tra mã sản phẩm.
 * Interface này là ranh giới truy cập dữ liệu cho service, gom các thao tác đọc/ghi cần thiết. Cách thực
 * thi bằng JPA hay JDBC nằm trong repository/adapter, không đặt trong controller.
 */
public interface SkuCatalogProjectionRepository extends JpaRepository<SkuCatalogProjection, String> {
    List<SkuCatalogProjection> findAllBySkuCodeIn(Collection<String> skuCodes);
}
