package com.dtthuan3.ecommerce.inventoryservice.integration;

import com.dtthuan3.ecommerce.inventoryservice.integration.event.ReturnLineApprovalEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Nhận event quyết định trả hàng từ topic nội bộ Return/Order.
 * Listener chỉ đọc/parse sự kiện và chuyển cho lớp integration-service; ghi inbox/projection nằm trong transaction
 * riêng để Kafka retry không làm tăng lượng hàng được phép nhận nhiều lần.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReturnApprovalKafkaListener {
    private final ObjectMapper objectMapper;
    private final InventoryReturnApprovalEventService eventService;
    private final MeterRegistry meterRegistry;

    /** Topic chỉ chứa event duyệt dòng trả; message key chuẩn là returnRequestId:orderLineId. */
    @KafkaListener(topics = "${inventory.kafka.return-approval-events-topic}",
            concurrency = "${inventory.kafka.consumer-concurrency}")
    public void onApprovalEvent(String payload) {
        Timer.Sample timer = Timer.start(meterRegistry);
        String outcome = "success";
        meterRegistry.counter("inventory.return_approval.events.received").increment();
        try {
            ReturnLineApprovalEvent event;
            try {
                event = objectMapper.readValue(payload, ReturnLineApprovalEvent.class);
            } catch (JsonProcessingException exception) {
                throw new IllegalArgumentException("Không thể đọc ReturnLineApprovalEvent", exception);
            }
            InventoryReturnApprovalEventService.Outcome result = eventService.apply(event);
            log.debug("Đã xử lý event duyệt dòng trả: eventId={}, returnRequestId={}, orderLineId={}, outcome={}",
                    event.eventId(), event.returnRequestId(), event.orderLineId(), result);
        } catch (RuntimeException failure) {
            outcome = "failure";
            meterRegistry.counter("inventory.return_approval.events.failures").increment();
            // Ném lại để DefaultErrorHandler retry hữu hạn rồi chuyển vào DLT của topic tương ứng.
            throw failure;
        } finally {
            timer.stop(Timer.builder("inventory.return_approval.events.processing")
                    .tag("outcome", outcome).register(meterRegistry));
        }
    }
}
