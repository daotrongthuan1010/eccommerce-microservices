package com.dtthuan3.ecommerce.inventoryservice.service;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.*;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.TransferInventoryResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.MovementType;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.StockMovementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Hợp đồng các nghiệp vụ tồn kho: tra cứu, nhập, giữ, xuất, điều chỉnh, điều chuyển và xem lịch sử.
 * Interface này công bố các use case Inventory cho controller, worker và adapter gọi tới. Class triển khai
 * nằm ở service/impl, vì vậy bên gọi phụ thuộc vào hợp đồng thay vì chi tiết xử lý.
 */
public interface InventoryService {
    InventoryResponse receiveStock(ReceiveStockRequest request, String performedBy, String operationId);
    void receiveClassifiedStock(List<ClassifiedStockReceipt> requests);
    InventoryResponse getById(String id);

    InventoryResponse getByPosition(
            String enterpriseId,
            String warehouseId,
            String locationId,
            String sku
    );

    InventoryResponse create(CreateInventoryRequest request);
    List<InventoryResponse> createBatch(List<CreateInventoryRequest> requests);
    InventoryResponse receive(ReceiveInventoryRequest request, String operationId);
    InventoryResponse reserve(ReserveInventoryRequest request);
    InventoryResponse release(ReleaseInventoryRequest request);
    InventoryResponse issue(IssueInventoryRequest request, String operationId);
    InventoryResponse adjust(AdjustInventoryRequest request, String operationId);

    TransferInventoryResponse transfer(TransferInventoryRequest request);

    Page<InventoryResponse> bySku(String sku, Pageable pageable);

    Page<InventoryResponse> byWarehouse(String warehouseId, Pageable pageable);

    Page<InventoryResponse> byLocation(String locationId, Pageable pageable);

    Page<InventoryResponse> available(String sku, String warehouseId, String locationId, Pageable pageable);

    Page<StockMovementResponse> movementHistory(String inventoryId, MovementType type,
                                                String referenceId, LocalDateTime from,
                                                LocalDateTime to, Pageable pageable);
}
