package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.inventoryservice.config.InventoryBatchProperties;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReceiveInventoryRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.Inventory;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryStockOperationRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryStockOperationRepository.StockOperationRecord;
import com.dtthuan3.ecommerce.inventoryservice.repository.StockMovementRepository;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryAvailabilityReadCache;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryLocationAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

/** Kiểm tra API nhập trực tiếp không cộng hàng lần hai khi client retry cùng key và payload. */
class InventoryStockOperationIdempotencyTest {

    private InventoryRepository inventoryRepository;
    private StockMovementRepository movementRepository;
    private InventoryLocationAccessService locationAccessService;
    private InventoryStockOperationRepository operationRepository;
    private InventoryServiceImpl service;
    private AtomicBoolean firstClaim;
    private AtomicReference<String> payloadHash;
    private AtomicReference<StockOperationRecord> savedOperation;
    private Inventory before;
    private Inventory after;

    @BeforeEach
    void setUp() {
        inventoryRepository = mock(InventoryRepository.class);
        movementRepository = mock(StockMovementRepository.class);
        locationAccessService = mock(InventoryLocationAccessService.class);
        operationRepository = mock(InventoryStockOperationRepository.class);
        firstClaim = new AtomicBoolean(true);
        payloadHash = new AtomicReference<>();
        savedOperation = new AtomicReference<>();
        service = new InventoryServiceImpl(inventoryRepository, movementRepository,
                new InventoryBatchProperties(500), locationAccessService, mock(DeadlockRetryExecutor.class),
                mock(InventoryAvailabilityReadCache.class), operationRepository,
                new ObjectMapper().findAndRegisterModules());
        ReflectionTestUtils.setField(service, "idempotencyKeyMaxLength", 120);

        before = inventory("10.0000", 4L);
        after = inventory("12.0000", 5L);
        when(inventoryRepository.findInventoryRowById("inventory-1")).thenReturn(Optional.of(before));
        when(inventoryRepository.findInventoryRowByIdForUpdate("inventory-1")).thenReturn(Optional.of(before));
        when(inventoryRepository.receive("inventory-1", new BigDecimal("2"))).thenReturn(after);
        when(operationRepository.tryStart(anyString(), eq("RECEIVE_EXISTING"), any(), anyString(), any()))
                .thenAnswer(invocation -> {
                    payloadHash.set(invocation.getArgument(3));
                    return firstClaim.getAndSet(false);
                });
        when(operationRepository.findByOperationId("receive-key-1"))
                .thenAnswer(invocation -> Optional.ofNullable(savedOperation.get()));
        doAnswer(invocation -> {
                    savedOperation.set(new StockOperationRecord("RECEIVE_EXISTING", "inventory-1",
                            payloadHash.get(), invocation.getArgument(2)));
                    return null;
                }).when(operationRepository).complete(eq("receive-key-1"), eq("inventory-1"), anyString());
    }

    @Test
    void retryReturnsSavedSnapshotWithoutApplyingReceiveTwice() {
        ReceiveInventoryRequest request = new ReceiveInventoryRequest("inventory-1", new BigDecimal("2"), 4L);

        InventoryResponse first = service.receive(request, "receive-key-1");
        InventoryResponse retry = service.receive(request, "receive-key-1");

        assertEquals(first, retry);
        assertEquals(new BigDecimal("12.0000"), retry.physicalQuantity());
        verify(inventoryRepository, times(1)).receive("inventory-1", new BigDecimal("2"));
        verify(movementRepository, times(1)).insertMovement(any());
        verify(operationRepository, times(1)).complete(eq("receive-key-1"), eq("inventory-1"), anyString());
    }

    @Test
    void sameKeyWithDifferentPayloadIsRejectedBeforeBalanceChanges() {
        ReceiveInventoryRequest original = new ReceiveInventoryRequest("inventory-1", new BigDecimal("2"), 4L);
        service.receive(original, "receive-key-1");

        ReceiveInventoryRequest changed = new ReceiveInventoryRequest("inventory-1", new BigDecimal("3"), 4L);
        assertThrows(BusinessException.class, () -> service.receive(changed, "receive-key-1"));

        verify(inventoryRepository, never()).receive("inventory-1", new BigDecimal("3"));
        verify(movementRepository, times(1)).insertMovement(any());
    }

    private Inventory inventory(String physicalQuantity, long version) {
        LocalDateTime now = LocalDateTime.now();
        return Inventory.builder().id("inventory-1").enterpriseId("enterprise-1").warehouseId("warehouse-1")
                .locationId("location-1").sku("SKU-1").physicalQuantity(new BigDecimal(physicalQuantity))
                .reservedQuantity(BigDecimal.ZERO).defectiveQuantity(BigDecimal.ZERO)
                .pendingQuantity(BigDecimal.ZERO).inTransitQuantity(BigDecimal.ZERO).version(version)
                .createdAt(now).updatedAt(now).build();
    }
}
