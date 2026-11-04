package com.dtthuan3.ecommerce.catalogservice.service.impl;

import com.dtthuan3.ecommerce.catalogservice.dto.search.ProductSearchReplayStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Replays the currently retained product event range to rebuild missing search documents. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchEventReplayService {

    private static final String DLT_TOPIC = "product-search-projection.DLT";
    private final KafkaProperties kafkaProperties;
    private final ProductSearchProjectionListener projectionListener;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicReference<Run> latestRun = new AtomicReference<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "catalog-product-search-replay");
        thread.setDaemon(true);
        return thread;
    });

    @Value("${catalog.search.events.product-topic:product-events}")
    private String productEventsTopic;

    @Value("${catalog.search.replay.max-events-per-second:5}")
    private int maxEventsPerSecond;

    public ProductSearchReplayStatus start() {
        if (!running.compareAndSet(false, true)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đang có một lượt replay search chạy");
        }
        Run run = new Run(UUID.randomUUID(), Instant.now());
        latestRun.set(run);
        executor.submit(() -> replay(run));
        return run.snapshot();
    }

    public ProductSearchReplayStatus status(UUID runId) {
        Run run = latestRun.get();
        if (run == null || !run.id.equals(runId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lượt replay search");
        }
        return run.snapshot();
    }

    private void replay(Run run) {
        Map<String, Object> properties = kafkaProperties.buildConsumerProperties();
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "catalog-search-replay-" + run.id);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(List.of(productEventsTopic));
            long assignmentDeadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
            while (consumer.assignment().isEmpty() && System.nanoTime() < assignmentDeadline) {
                consumer.poll(Duration.ofMillis(250));
            }
            if (consumer.assignment().isEmpty()) {
                throw new IllegalStateException("Kafka chưa cấp partition cho product event topic");
            }

            var assignments = consumer.assignment();
            consumer.seekToBeginning(assignments);
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(assignments);
            while (hasUnprocessedPartitions(consumer, endOffsets)) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    if (record.offset() >= endOffsets.getOrDefault(
                            new TopicPartition(record.topic(), record.partition()), 0L)) continue;
                    try {
                        projectionListener.onProductEvent(record.value());
                    } catch (Exception projectionFailure) {
                        publishToDlt(record, projectionFailure);
                        run.failedEvents++;
                        run.lastError = projectionFailure.getMessage();
                    }
                    run.processedEvents++;
                    throttle();
                }
            }
            run.status = run.failedEvents == 0 ? "COMPLETED" : "COMPLETED_WITH_ERRORS";
        } catch (Exception ex) {
            run.status = "FAILED";
            run.lastError = ex.getMessage();
            log.error("Catalog retained-event search replay failed, runId={}", run.id, ex);
        } finally {
            run.finishedAt = Instant.now();
            running.set(false);
        }
    }

    private boolean hasUnprocessedPartitions(KafkaConsumer<String, String> consumer,
                                             Map<TopicPartition, Long> endOffsets) {
        for (var entry : endOffsets.entrySet()) {
            if (consumer.position(entry.getKey()) < entry.getValue()) return true;
        }
        return false;
    }

    private void publishToDlt(ConsumerRecord<String, String> record, Exception cause) throws Exception {
        kafkaTemplate.send(DLT_TOPIC, record.key(), record.value()).get(5, java.util.concurrent.TimeUnit.SECONDS);
        log.warn("Retained-event replay sent event to DLT: partition={}, offset={}, reason={}",
                record.partition(), record.offset(), cause.getMessage());
    }

    private void throttle() throws InterruptedException {
        long safeRps = Math.max(1, maxEventsPerSecond);
        Thread.sleep(Math.max(1, 1_000L / safeRps));
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private static final class Run {
        private final UUID id;
        private final Instant startedAt;
        private volatile String status = "RUNNING";
        private volatile long processedEvents;
        private volatile long failedEvents;
        private volatile Instant finishedAt;
        private volatile String lastError;

        private Run(UUID id, Instant startedAt) { this.id = id; this.startedAt = startedAt; }

        private ProductSearchReplayStatus snapshot() {
            return new ProductSearchReplayStatus(id, status, processedEvents, failedEvents,
                    startedAt, finishedAt, lastError);
        }
    }
}
