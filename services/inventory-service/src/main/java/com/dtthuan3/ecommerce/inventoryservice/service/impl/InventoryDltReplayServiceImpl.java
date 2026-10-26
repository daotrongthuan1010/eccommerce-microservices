package com.dtthuan3.ecommerce.inventoryservice.service.impl;

import com.dtthuan3.ecommerce.common.exception.BusinessException;
import com.dtthuan3.ecommerce.common.exception.ErrorCode;
import com.dtthuan3.ecommerce.inventoryservice.dto.request.ReplayProductEventDltRequest;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.ReplayProductEventDltResponse;
import com.dtthuan3.ecommerce.inventoryservice.service.InventoryDltReplayService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Đọc đúng một record DLT, khóa replay theo partition và chờ broker xác nhận trước khi trả kết quả. */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryDltReplayServiceImpl implements InventoryDltReplayService {
    private static final int ADVISORY_LOCK_NAMESPACE = 726218;
    private static final int DLT_REPLAY_LOCK_KEY = 0;

    private final ConsumerFactory<Object, Object> consumerFactory;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${inventory.kafka.product-events-topic:product-events}")
    private String sourceTopic;

    @Value("${inventory.kafka.product-events-dlt-topic:product-events.DLT}")
    private String dltTopic;

    @Value("${inventory.kafka.dlt-replay-poll-timeout:PT10S}")
    private Duration pollTimeout;

    @Value("${inventory.kafka.dlt-replay-send-timeout:PT130S}")
    private Duration sendTimeout;

    @Override
    public ReplayProductEventDltResponse replayProductEvent(
            ReplayProductEventDltRequest request, String operator) {
        String replayRequestId = UUID.randomUUID().toString();
        meterRegistry.counter("inventory.kafka.dlt_replay.requests").increment();

        try {
            ReplayProductEventDltResponse response = jdbcTemplate.execute(
                    (ConnectionCallback<ReplayProductEventDltResponse>) connection ->
                            replayWithPartitionLock(connection, request, operator, replayRequestId));
            meterRegistry.counter("inventory.kafka.dlt_replay.published").increment();
            return response;
        } catch (BusinessException failure) {
            meterRegistry.counter("inventory.kafka.dlt_replay.failures", "reason", failure.getCode()).increment();
            throw failure;
        } catch (DataAccessException failure) {
            meterRegistry.counter("inventory.kafka.dlt_replay.failures", "reason", "database").increment();
            log.error("Không lấy được khóa replay DLT, replayRequestId={}", replayRequestId, failure);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                    "Không thể khóa partition DLT để replay; hãy thử lại sau");
        }
    }

    /** Advisory lock toàn cục của PostgreSQL chỉ cho phép một replay chạy giữa mọi Inventory instance. */
    private ReplayProductEventDltResponse replayWithPartitionLock(
            Connection connection,
            ReplayProductEventDltRequest request,
            String operator,
            String replayRequestId) throws SQLException {
        if (!tryAcquireReplayLock(connection)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Đang có một thao tác replay Product DLT khác");
        }

        try {
            return readAndPublish(request, operator, replayRequestId);
        } finally {
            releaseReplayLock(connection, replayRequestId);
        }
    }

    private ReplayProductEventDltResponse readAndPublish(
            ReplayProductEventDltRequest request,
            String operator,
            String replayRequestId) {
        TopicPartition topicPartition = new TopicPartition(dltTopic, request.partition());
        ConsumerRecord<Object, Object> dltRecord = readDltRecord(topicPartition, request.offset());
        validateSourceCoordinates(dltRecord, request.partition());

        if (!(dltRecord.value() instanceof String payload)) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                    "DLT record không có payload dạng chuỗi để replay");
        }
        if (dltRecord.key() != null && !(dltRecord.key() instanceof String)) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                    "DLT record có key không phải chuỗi nên không thể replay nguyên trạng");
        }
        String key = (String) dltRecord.key();

        try {
            SendResult<String, String> result = kafkaTemplate
                    .send(sourceTopic, request.partition(), key, payload)
                    .get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
            long targetOffset = result.getRecordMetadata().offset();
            log.info("Replay Product DLT thành công: replayRequestId={}, operator={}, reason={}, "
                            + "dltTopic={}, partition={}, dltOffset={}, sourceTopic={}, targetOffset={}",
                    replayRequestId, operator, request.reason(), dltTopic, request.partition(),
                    request.offset(), sourceTopic, targetOffset);
            return new ReplayProductEventDltResponse(replayRequestId, dltTopic,
                    request.partition(), request.offset(), sourceTopic,
                    result.getRecordMetadata().partition(), targetOffset, key);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw publishFailure(replayRequestId, request, operator, failure);
        } catch (ExecutionException | TimeoutException failure) {
            // Nếu HTTP bị timeout, gửi có thể đã tới broker; phát lại eventId/payload gốc vẫn an toàn nhờ inbox.
            throw publishFailure(replayRequestId, request, operator, failure);
        } catch (RuntimeException failure) {
            throw publishFailure(replayRequestId, request, operator, failure);
        }
    }

    private ConsumerRecord<Object, Object> readDltRecord(TopicPartition topicPartition, long offset) {
        String clientSuffix = UUID.randomUUID().toString();
        try (Consumer<Object, Object> consumer = consumerFactory.createConsumer(
                "inventory-dlt-replay", "inventory-dlt-replay", clientSuffix)) {
            Set<TopicPartition> assignment = Set.of(topicPartition);
            consumer.assign(assignment);

            long beginningOffset = consumer.beginningOffsets(assignment).get(topicPartition);
            long nextOffset = consumer.endOffsets(assignment).get(topicPartition);
            if (offset < beginningOffset || offset >= nextOffset) {
                throw new BusinessException(ErrorCode.NOT_FOUND,
                        "Không có DLT record tại offset đã chọn hoặc record đã hết retention");
            }

            consumer.seek(topicPartition, offset);
            long deadline = System.nanoTime() + pollTimeout.toNanos();
            while (System.nanoTime() < deadline) {
                ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofMillis(250));
                for (ConsumerRecord<Object, Object> record : records.records(topicPartition)) {
                    if (record.offset() == offset) return record;
                    if (record.offset() > offset) break;
                }
                if (consumer.position(topicPartition) > offset) break;
            }
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    "Không tìm thấy DLT record chính xác tại offset đã chọn");
        } catch (BusinessException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            log.error("Không đọc được Product DLT: topic={}, partition={}, offset={}",
                    dltTopic, topicPartition.partition(), offset, failure);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                    "Không thể đọc Product DLT; hãy kiểm tra broker, topic và quyền truy cập");
        }
    }

    /** Không cho endpoint phát nhầm record từ DLT khác hoặc sai partition nguồn. */
    private void validateSourceCoordinates(ConsumerRecord<Object, Object> record, int requestedPartition) {
        Header sourceTopicHeader = record.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC);
        Header sourcePartitionHeader = record.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_PARTITION);
        if (sourceTopicHeader == null || sourcePartitionHeader == null) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                    "DLT record thiếu metadata topic/partition nguồn nên không thể replay an toàn");
        }

        String originalTopic = new String(sourceTopicHeader.value(), StandardCharsets.UTF_8);
        if (!sourceTopic.equals(originalTopic)) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                    "DLT record không thuộc Product topic đang cấu hình");
        }
        byte[] partitionBytes = sourcePartitionHeader.value();
        if (partitionBytes == null || partitionBytes.length != Integer.BYTES
                || ByteBuffer.wrap(partitionBytes).getInt() != requestedPartition
                || record.partition() != requestedPartition) {
            throw new BusinessException(ErrorCode.INVALID_OPERATION_CONTENT,
                    "Partition DLT không khớp partition nguồn được yêu cầu");
        }
    }

    private BusinessException publishFailure(
            String replayRequestId,
            ReplayProductEventDltRequest request,
            String operator,
            Exception failure) {
        log.error("Replay Product DLT chưa nhận được xác nhận gửi; replayRequestId={}, operator={}, "
                        + "reason={}, partition={}, offset={}",
                replayRequestId, operator, request.reason(), request.partition(), request.offset(), failure);
        return new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                "Chưa xác nhận được broker đã nhận event; replayRequestId=" + replayRequestId
                        + ". Có thể retry cùng partition/offset vì inbox chống trùng");
    }

    private boolean tryAcquireReplayLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT pg_try_advisory_lock(?, ?)")) {
            statement.setInt(1, ADVISORY_LOCK_NAMESPACE);
            statement.setInt(2, DLT_REPLAY_LOCK_KEY);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }

    private void releaseReplayLock(Connection connection, String replayRequestId) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT pg_advisory_unlock(?, ?)")) {
            statement.setInt(1, ADVISORY_LOCK_NAMESPACE);
            statement.setInt(2, DLT_REPLAY_LOCK_KEY);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || !resultSet.getBoolean(1)) {
                    log.error("PostgreSQL không xác nhận nhả khóa replay, replayRequestId={}", replayRequestId);
                }
            }
        } catch (SQLException failure) {
            // Nếu không thể nhả khóa theo partition, dọn tất cả session advisory lock trước khi trả connection.
            try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_unlock_all()")) {
                statement.execute();
            } catch (SQLException unlockAllFailure) {
                failure.addSuppressed(unlockAllFailure);
                try {
                    connection.abort(Runnable::run);
                } catch (SQLException abortFailure) {
                    failure.addSuppressed(abortFailure);
                }
            }
            log.error("Lỗi khi nhả khóa replay DLT: replayRequestId={}", replayRequestId, failure);
        }
    }
}
