package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryEventInboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * PostgreSQL adapter dùng unique key và ON CONFLICT để khử trùng lặp an toàn giữa nhiều instance.
 * Class/interface này chứa hợp đồng hoặc phần thực thi truy vấn SQL/JDBC tùy chỉnh như khóa dòng, cập nhật có điều kiện và phân trang; repository tập trung thao tác dữ liệu, không điều phối toàn bộ use case.
 */
@Repository
@RequiredArgsConstructor
public class JdbcInventoryEventInboxRepository implements InventoryEventInboxRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public InboxClaim claim(String consumerName, String eventId, String payloadHash, Instant processedAt) {
        // Unique key (consumer_name, event_id) và ON CONFLICT chọn một winner ngay tại PostgreSQL,
        // vì vậy hai instance nhận cùng event đồng thời không thể cùng claim thành công.
        int inserted = jdbcTemplate.update("""
                        INSERT INTO inventory_event_inbox (
                            id, consumer_name, event_id, payload_hash, processed_at
                        ) VALUES (?, ?, ?, ?, ?)
                        ON CONFLICT (consumer_name, event_id) DO NOTHING
                        """,
                UUID.randomUUID().toString(), consumerName, eventId, payloadHash,
                Timestamp.from(processedAt));
        if (inserted == 1) return new InboxClaim(true, payloadHash);

        // Đọc hash đã lưu để caller kiểm tra duplicate có cùng payload hay eventId bị dùng sai nội dung.
        List<String> hashes = jdbcTemplate.query("""
                        SELECT payload_hash FROM inventory_event_inbox
                        WHERE consumer_name = ? AND event_id = ?
                        """,
                (resultSet, rowNumber) -> resultSet.getString("payload_hash"), consumerName, eventId);
        if (hashes.isEmpty()) {
            throw new IllegalStateException("Inbox event biến mất trong lúc kiểm tra eventId=" + eventId);
        }
        return new InboxClaim(false, hashes.getFirst());
    }

    @Override
    public int deleteProcessedBefore(Instant cutoff, int limit) {
        if (limit < 1) return 0;
        // Mỗi câu lệnh chỉ khóa và xóa số lượng hữu hạn; nhiều instance có thể dọn song song mà không chờ nhau.
        return jdbcTemplate.update("""
                        WITH delete_batch AS (
                            SELECT id
                            FROM inventory_event_inbox
                            WHERE processed_at < ?
                            ORDER BY processed_at, id
                            LIMIT ?
                            FOR UPDATE SKIP LOCKED
                        )
                        DELETE FROM inventory_event_inbox AS inbox
                        USING delete_batch
                        WHERE inbox.id = delete_batch.id
                        """,
                Timestamp.from(cutoff), limit);
    }
}
