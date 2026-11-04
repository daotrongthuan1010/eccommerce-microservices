package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.CreateInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryReservationResponse;
import com.dtthuan3.ecommerce.inventoryservice.entity.Inventory;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReservationRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReservationRepository.ReservationHeaderRecord;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReservationRepository.ReservationLineRecord;
import com.dtthuan3.ecommerce.inventoryservice.repository.StockMovementRepository;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryLocationAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Kiểm tra retry tạo reservation không giữ tồn hai lần và không cho tái dùng operationId với payload khác. */
class InventoryReservationServiceIdempotencyTest {

    private InventoryReservationRepository reservationRepository;
    private InventoryRepository inventoryRepository;
    private InventoryLocationAccessService locationAccessService;
    private StockMovementRepository movementRepository;
    private InventoryReservationServiceImpl service;
    private AtomicReference<ReservationHeaderRecord> savedHeader;
    private AtomicReference<List<ReservationLineRecord>> savedLines;
    private Instant expiresAt;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(InventoryReservationRepository.class);
        inventoryRepository = mock(InventoryRepository.class);
        locationAccessService = mock(InventoryLocationAccessService.class);
        movementRepository = mock(StockMovementRepository.class);
        savedHeader = new AtomicReference<>();
        savedLines = new AtomicReference<>(List.of());
        expiresAt = Instant.now().plusSeconds(600);

        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenAnswer(invocation -> new SimpleTransactionStatus());
        doNothing().when(transactionManager).commit(any());
        doNothing().when(transactionManager).rollback(any());
        DeadlockRetryExecutor retryExecutor = new DeadlockRetryExecutor(
                transactionManager, 3, Duration.ofMillis(40), Duration.ofSeconds(1));
        service = new InventoryReservationServiceImpl(reservationRepository, inventoryRepository,
                locationAccessService, movementRepository, retryExecutor);

        when(reservationRepository.findByOperationId("operation-1"))
                .thenAnswer(invocation -> Optional.ofNullable(savedHeader.get()));
        when(reservationRepository.findById("reservation-1")).thenReturn(Optional.empty());
        when(reservationRepository.findByIdForUpdate("reservation-1"))
                .thenAnswer(invocation -> Optional.ofNullable(savedHeader.get()));
        when(reservationRepository.findLines("reservation-1"))
                .thenAnswer(invocation -> savedLines.get());
        when(reservationRepository.insertHeader(any(ReservationHeaderRecord.class)))
                .thenAnswer(invocation -> {
                    savedHeader.set(invocation.getArgument(0));
                    return true;
                });
        doAnswer(invocation -> {
            savedLines.set(invocation.getArgument(1));
            return null;
        }).when(reservationRepository).insertLines(eq("reservation-1"), anyList());
        when(reservationRepository.toResponse(any(ReservationHeaderRecord.class), anyList()))
                .thenCallRealMethod();

        Inventory before = inventory("inventory-1", new BigDecimal("10"), BigDecimal.ZERO, 0L);
        Inventory after = inventory("inventory-1", new BigDecimal("10"), BigDecimal.ONE, 1L);
        when(inventoryRepository.findByIdsForUpdate(List.of("inventory-1"))).thenReturn(List.of(before));
        when(inventoryRepository.reserve("inventory-1", BigDecimal.ONE)).thenReturn(Optional.of(after));
    }

    @Test
    void retryWithSameOperationAndPayloadReturnsExistingReservationWithoutChangingStockAgain() {
        CreateInventoryReservationRequest request = request(BigDecimal.ONE);

        InventoryReservationResponse first = service.reserve(request, "test-user");
        InventoryReservationResponse retry = service.reserve(request, "test-user");

        assertNotNull(first);
        assertNotNull(retry);
        assertEquals(first.reservationId(), retry.reservationId());
        assertEquals("reservation-1", retry.reservationId());
        verify(inventoryRepository, times(1)).reserve("inventory-1", BigDecimal.ONE);
        verify(reservationRepository, times(1)).insertHeader(any(ReservationHeaderRecord.class));
        verify(reservationRepository, times(1)).insertLines(eq("reservation-1"), anyList());
        verify(movementRepository, times(1)).insertMovements(anyList());
    }

    @Test
    void retryWithSameOperationButDifferentQuantityIsRejected() {
        service.reserve(request(BigDecimal.ONE), "test-user");

        assertThrows(BusinessException.class, () -> service.reserve(request(new BigDecimal("2")), "test-user"));

        verify(inventoryRepository, times(1)).reserve("inventory-1", BigDecimal.ONE);
        verify(reservationRepository, times(1)).insertHeader(any(ReservationHeaderRecord.class));
    }

    private CreateInventoryReservationRequest request(BigDecimal quantity) {
        return new CreateInventoryReservationRequest("reservation-1", "order-1", "operation-1",
                expiresAt,
                List.of(new CreateInventoryReservationRequest.Line("inventory-1", quantity)));
    }

    private Inventory inventory(String id, BigDecimal physical, BigDecimal reserved, long version) {
        return Inventory.builder().id(id).enterpriseId("enterprise-1").warehouseId("warehouse-1")
                .locationId("location-1").sku("SKU-1").physicalQuantity(physical)
                .reservedQuantity(reserved).defectiveQuantity(BigDecimal.ZERO).pendingQuantity(BigDecimal.ZERO)
                .inTransitQuantity(BigDecimal.ZERO).version(version).build();
    }
}
