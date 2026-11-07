package com.dtthuan3.ecommerce.inventoryservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReleaseInventoryReservationRequest;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReservationRepository;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryReservationRepository.ReservationHeaderRecord;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * SAGA-UC04 (issue #4, ADR-0001): compensation phia inventory — event
 * OrderPaymentFailed phai release reservation HELD cua don ngay.
 * Truoc fix: khong ai lang nghe event nay, hang bi giu den TTL 15 phut.
 */
class OrderPaymentFailedListenerTest {

    private static final String COMPENSATION_EVENT = """
            {"eventId":"evt-c1","eventType":"OrderPaymentFailed","orderId":"order-1",
             "orderCode":"ORD-001","userId":"user-1","status":"PAYMENT_FAILED",
             "paymentEventType":"PaymentFailed","occurredAt":"2026-11-07T10:00:00Z"}
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private InventoryReservationService reservationService;
    private InventoryReservationRepository reservationRepository;
    private OrderPaymentFailedListener listener;

    @BeforeEach
    void setUp() {
        reservationService = mock(InventoryReservationService.class);
        reservationRepository = mock(InventoryReservationRepository.class);
        listener = new OrderPaymentFailedListener(objectMapper, reservationService, reservationRepository);
    }

    private ReservationHeaderRecord heldReservation() {
        return new ReservationHeaderRecord("resv-1", "order-1", "ent-1", "op-1", "hash",
                "HELD", Instant.now().plusSeconds(600), 3L, "user-1", Instant.now(), null, null, null);
    }

    @Test
    @DisplayName("SAGA-UC04: OrderPaymentFailed -> release reservation HELD voi operationId idempotent")
    void paymentFailed_releasesHeldReservation() {
        when(reservationRepository.findHeldByOrderId("order-1"))
                .thenReturn(Optional.of(heldReservation()));

        listener.onOrderEvent(COMPENSATION_EVENT);

        ArgumentCaptor<ReleaseInventoryReservationRequest> request =
                ArgumentCaptor.forClass(ReleaseInventoryReservationRequest.class);
        verify(reservationService).release(eq("resv-1"), request.capture(), anyString());

        // operationId on dinh theo orderId: event den lai khong tao release thu 2
        assertThat(request.getValue().operationId()).isEqualTo("release-order-order-1");
        // release toan bo residual (lines trong)
        assertThat(request.getValue().lines()).isEmpty();
        // expectedVersion dung version hien tai cua header
        assertThat(request.getValue().expectedVersion()).isEqualTo(3L);
    }

    @Test
    @DisplayName("SAGA-UC05: don khong con reservation HELD -> bo qua an toan (idempotent)")
    void noHeldReservation_skipsSilently() {
        when(reservationRepository.findHeldByOrderId("order-1")).thenReturn(Optional.empty());

        listener.onOrderEvent(COMPENSATION_EVENT);

        verify(reservationService, never()).release(anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Event khac (OrderConfirmed...) -> bo qua, khong release nham")
    void unrelatedEvent_ignored() {
        String other = COMPENSATION_EVENT.replace("OrderPaymentFailed", "OrderConfirmed");

        listener.onOrderEvent(other);

        verify(reservationRepository, never()).findHeldByOrderId(anyString());
    }

    @Test
    @DisplayName("ADR-0002: release fail -> rethrow (retry/DLT), khong nuot exception")
    void releaseFailure_rethrows() {
        when(reservationRepository.findHeldByOrderId("order-1"))
                .thenReturn(Optional.of(heldReservation()));
        when(reservationService.release(anyString(), any(), anyString()))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class, () -> listener.onOrderEvent(COMPENSATION_EVENT));
    }
}
