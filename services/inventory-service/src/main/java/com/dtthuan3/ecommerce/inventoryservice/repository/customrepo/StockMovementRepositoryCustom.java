package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.dto.response.StockMovementResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.MovementType;
import com.dtthuan3.ecommerce.inventoryservice.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Hợp đồng truy vấn SQL cho việc ghi và đọc lịch sử movement.
 * Class/interface này chứa hợp đồng hoặc phần thực thi truy vấn SQL/JDBC tùy chỉnh như khóa dòng, cập nhật có điều kiện và phân trang; repository tập trung thao tác dữ liệu, không điều phối toàn bộ use case.
 */
@Repository
public interface StockMovementRepositoryCustom {
    void insertMovement(StockMovement movement);

    /** Ghi nhiều movement trong một JDBC batch, cùng transaction với cập nhật tồn. */
    void insertMovements(List<StockMovement> movements);

    Page<StockMovementResponse>
    searchMovements(String inventoryId, MovementType type,
                    String referenceId, LocalDateTime from, LocalDateTime to,
                    Pageable pageable);
}
