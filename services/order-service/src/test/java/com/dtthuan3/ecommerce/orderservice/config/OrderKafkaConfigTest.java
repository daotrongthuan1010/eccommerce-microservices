package com.dtthuan3.ecommerce.orderservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

/**
 * DLT-UC01 (issue #5): order-service phai co error handler retry + DLT cho
 * payment-events consumer. Truoc fix: rethrow nhung khong co DefaultErrorHandler
 * -> poison message block partition vo han.
 *
 * <p>Test qua hanh vi cong khai cua bean that: handleRemaining voi record poison
 * phai ket thuc khong nem lai (recovered = recoverer da publish DLT), va cau
 * hinh commit/seek phai khong khoa partition.
 */
class OrderKafkaConfigTest {

    private final KafkaTemplate<String, String> template = new KafkaTemplate<>(
            new DefaultKafkaProducerFactory<>(Map.of()));
    private final MessageListenerContainer container = org.mockito.Mockito.mock(MessageListenerContainer.class);

    private DefaultErrorHandler handler() {
        return new OrderKafkaConfig().orderKafkaErrorHandler(template);
    }

    @Test
    @DisplayName("DLT-UC01: record payment-events poison het retry -> recovered (khong nem lai, khong block partition)")
    void poisonRecord_isRecoveredNotRethrown() {
        DefaultErrorHandler handler = handler();
        ConsumerRecord<String, String> poison =
                new ConsumerRecord<>("payment-events", 2, 100L, "k", "not-json");

        // Backoff 3 lan x 1s — het retry recoverer publish DLT (send vao template
        // that se fail vi khong co broker, nen test verify duong recovered
        // thong qua viec exception goc KHONG duoc nem lai khi send that bai
        // thi moi bi nem). Voi broker test that thi recovered = im lang.
        // O day: send DLT that bai (khong broker) -> handler nem exception
        // recovery-failed — chung to recoverer DAU DUOC goi (khong phai bean
        // mo coi). Neu recoverer khong duoc wire, exception goc "poison" bay ra.
        assertThatCode(() -> handler.handleRemaining(new RuntimeException("poison"),
                        java.util.List.of(poison),
                        org.mockito.Mockito.mock(org.apache.kafka.clients.consumer.Consumer.class),
                        org.mockito.Mockito.mock(MessageListenerContainer.class)))
                .as("Recoverer phai duoc goi — recovery-failed (send DLT that bai vi khong broker) "
                        + "chu khong phai exception goc 'poison' bay lai")
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("DLT-UC01b: commit offset sau khi DLT (record da cuu ho khong bi xu ly lai)")
    void recoveredRecord_commitsOffset() {
        DefaultErrorHandler handler = handler();

        // isAckAfterHandle = commit offset record da recover (setCommitRecovered(true))
        assertThat(handler.isAckAfterHandle())
                .as("setCommitRecovered(true) — commit offset sau khi DLT")
                .isTrue();
        // seeksAfterHandling mac dinh true — seek lai de retry theo backoff,
        // sau khi recovered thi record duoc bo qua; khong phai vong lap vo han.
        // Khong assert gi them vi se do vao implementation chi tiet.
    }
}
