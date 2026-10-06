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

public interface InventoryService {
    InventoryResponse receiveStock(ReceiveStockRequest request, String performedBy);
    InventoryResponse getById(String id);

    InventoryResponse getByPosition(
            String enterpriseId,
            String warehouseId,
            String locationId,
            String sku
    );

    InventoryResponse create(CreateInventoryRequest request);
    List<InventoryResponse> createBatch(List<CreateInventoryRequest> requests);
    InventoryResponse receive(ReceiveInventoryRequest request);
    InventoryResponse reserve(ReserveInventoryRequest request);
    InventoryResponse release(ReleaseInventoryRequest request);
    InventoryResponse issue(IssueInventoryRequest request);
    InventoryResponse adjust(AdjustInventoryRequest request);

    TransferInventoryResponse transfer(TransferInventoryRequest request);

    List<InventoryResponse> bySku(String sku);

    List<InventoryResponse> byWarehouse(String warehouseId);

    List<InventoryResponse> byLocation(String locationId);

    List<InventoryResponse> available(String sku, String warehouseId, String locationId);

    Page<StockMovementResponse> movementHistory(String inventoryId, MovementType type,
                                                String referenceId, LocalDateTime from,
                                                LocalDateTime to, Pageable pageable);
}
