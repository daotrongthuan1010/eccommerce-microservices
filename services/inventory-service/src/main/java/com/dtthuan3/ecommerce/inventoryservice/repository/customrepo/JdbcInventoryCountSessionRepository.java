package com.dtthuan3.ecommerce.inventoryservice.repository.customrepo;

import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSessionResponse;
import com.dtthuan3.ecommerce.inventoryservice.dto.response.InventoryCountSessionLineResponse;
import com.dtthuan3.ecommerce.inventoryservice.repository.InventoryCountSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Adapter JDBC để chụp snapshot theo lô, tránh tải toàn bộ balance lên heap của ứng dụng. */
@Repository
@RequiredArgsConstructor
public class JdbcInventoryCountSessionRepository implements InventoryCountSessionRepository {
    private final JdbcTemplate jdbcTemplate;

    /** Lấy trang session cùng snapshotLineCount bằng scalar subquery cố định, không phát sinh N+1. */
    @Override
    public Page<InventoryCountSessionResponse> searchSessions(String enterpriseId, String warehouseId,
                                                               String status, Pageable pageable) {
        StringBuilder where = new StringBuilder(" WHERE s.enterprise_id = ?");
        List<Object> filters = new ArrayList<>();
        filters.add(enterpriseId);
        if (warehouseId != null && !warehouseId.isBlank()) {
            where.append(" AND s.warehouse_id = ?");
            filters.add(warehouseId);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND s.status = ?");
            filters.add(status);
        }
        List<Object> queryParameters = new ArrayList<>(filters);
        queryParameters.add(pageable.getPageSize());
        queryParameters.add(pageable.getOffset());
        String columns = "SELECT s.id, s.enterprise_id, s.warehouse_id, s.location_id, s.sku, "
                + "s.status, s.reason, s.opened_by, s.snapshot_at, s.version, "
                + "(SELECT COUNT(*) FROM inventory_count_session_line l WHERE l.session_id = s.id) "
                + "AS snapshot_line_count FROM inventory_count_session s";
        List<InventoryCountSessionResponse> sessions = jdbcTemplate.query(columns + where
                        + " ORDER BY s.created_at DESC, s.id DESC LIMIT ? OFFSET ?",
                (rs, row) -> new InventoryCountSessionResponse(rs.getString("id"),
                        rs.getString("enterprise_id"), rs.getString("warehouse_id"),
                        rs.getString("location_id"), rs.getString("sku"), rs.getString("status"),
                        rs.getString("reason"), rs.getString("opened_by"),
                        rs.getTimestamp("snapshot_at").toLocalDateTime(),
                        rs.getLong("snapshot_line_count"), rs.getLong("version")), queryParameters.toArray());
        return PageableExecutionUtils.getPage(sessions, pageable, () -> {
            Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_count_session s" + where,
                    Long.class, filters.toArray());
            return total == null ? 0 : total;
        });
    }

    @Override
    public Optional<InventoryCountSessionResponse> findByOperation(String enterpriseId, String operationId) {
        List<InventoryCountSessionResponse> rows = jdbcTemplate.query("""
                SELECT s.id, s.enterprise_id, s.warehouse_id, s.location_id, s.sku,
                       s.status, s.reason, s.opened_by, s.snapshot_at, s.version,
                       (SELECT COUNT(*) FROM inventory_count_session_line l WHERE l.session_id = s.id)
                           AS snapshot_line_count
                FROM inventory_count_session s
                WHERE s.enterprise_id = ? AND s.operation_id = ?
                """, (rs, row) -> new InventoryCountSessionResponse(
                rs.getString("id"), rs.getString("enterprise_id"), rs.getString("warehouse_id"),
                rs.getString("location_id"), rs.getString("sku"), rs.getString("status"),
                rs.getString("reason"), rs.getString("opened_by"), rs.getTimestamp("snapshot_at").toLocalDateTime(),
                rs.getLong("snapshot_line_count"), rs.getLong("version")), enterpriseId, operationId);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<String> findPayloadHash(String enterpriseId, String operationId) {
        List<String> hashes = jdbcTemplate.query("""
                SELECT payload_hash FROM inventory_count_session
                WHERE enterprise_id = ? AND operation_id = ?
                """, (rs, row) -> rs.getString("payload_hash"), enterpriseId, operationId);
        return hashes.stream().findFirst();
    }

    @Override
    public boolean existsSession(String sessionId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM inventory_count_session WHERE id = ?)
                """, Boolean.class, sessionId);
        return Boolean.TRUE.equals(exists);
    }

    /** Lấy baseline theo trang và thứ tự ổn định; không tải mọi dòng tồn vào bộ nhớ ứng dụng. */
    @Override
    public Page<InventoryCountSessionLineResponse> findLines(String sessionId, Pageable pageable) {
        List<InventoryCountSessionLineResponse> content = jdbcTemplate.query("""
                SELECT l.inventory_id, l.sku, l.location_id, l.physical_quantity, l.reserved_quantity,
                       l.defective_quantity, l.pending_quantity, l.in_transit_quantity, l.inventory_version,
                       latest.counted_good_quantity, latest.counted_defective_quantity,
                       latest.counted_pending_quantity, latest.count_reason, latest.evidence_reference,
                       latest.counted_by
                FROM inventory_count_session_line l
                LEFT JOIN LATERAL (
                    SELECT o.counted_good_quantity, o.counted_defective_quantity, o.counted_pending_quantity,
                           o.count_reason, o.evidence_reference, o.counted_by
                    FROM inventory_count_observation o
                    JOIN inventory_count_submission s ON s.id = o.submission_id
                    WHERE o.session_id = l.session_id AND o.inventory_id = l.inventory_id
                    ORDER BY s.version_after DESC, o.counted_at DESC, o.id DESC
                    LIMIT 1
                ) latest ON TRUE
                WHERE l.session_id = ?
                ORDER BY l.sku, l.location_id, l.inventory_id
                LIMIT ? OFFSET ?
                """, (rs, row) -> new InventoryCountSessionLineResponse(
                rs.getString("inventory_id"), rs.getString("sku"), rs.getString("location_id"),
                rs.getBigDecimal("physical_quantity"), rs.getBigDecimal("reserved_quantity"),
                rs.getBigDecimal("defective_quantity"), rs.getBigDecimal("pending_quantity"),
                rs.getBigDecimal("in_transit_quantity"), rs.getLong("inventory_version"),
                rs.getBigDecimal("counted_good_quantity"), rs.getBigDecimal("counted_defective_quantity"),
                rs.getBigDecimal("counted_pending_quantity"), rs.getString("count_reason"),
                rs.getString("evidence_reference"), rs.getString("counted_by")),
                sessionId, pageable.getPageSize(), pageable.getOffset());
        return PageableExecutionUtils.getPage(content, pageable, () -> {
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM inventory_count_session_line WHERE session_id = ?
                    """, Long.class, sessionId);
            return count == null ? 0 : count;
        });
    }

    @Override
    public Optional<SessionState> lockSession(String sessionId) {
        List<SessionState> sessions = jdbcTemplate.query("""
                SELECT id, status, version FROM inventory_count_session
                WHERE id = ? FOR UPDATE
                """, (rs, row) -> new SessionState(rs.getString("id"), rs.getString("status"),
                rs.getLong("version")), sessionId);
        return sessions.stream().findFirst();
    }

    @Override
    public Optional<CountSubmissionRecord> findSubmission(String sessionId, String operationId) {
        List<CountSubmissionRecord> submissions = jdbcTemplate.query("""
                SELECT id, session_id, operation_id, payload_hash, line_count, version_after,
                       recorded_by, created_at
                FROM inventory_count_submission
                WHERE session_id = ? AND operation_id = ?
                """, (rs, row) -> new CountSubmissionRecord(
                rs.getString("id"), rs.getString("session_id"), rs.getString("operation_id"),
                rs.getString("payload_hash"), rs.getInt("line_count"), rs.getLong("version_after"),
                rs.getString("recorded_by"), rs.getTimestamp("created_at").toLocalDateTime()),
                sessionId, operationId);
        return submissions.stream().findFirst();
    }

    @Override
    public long countLinesBelongingToSession(String sessionId, List<String> inventoryIds) {
        if (inventoryIds.isEmpty()) return 0;
        String placeholders = String.join(",", Collections.nCopies(inventoryIds.size(), "?"));
        ArrayList<Object> args = new ArrayList<>();
        args.add(sessionId);
        args.addAll(inventoryIds);
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM inventory_count_session_line
                WHERE session_id = ? AND inventory_id IN (
                """ + placeholders + ")", Long.class, args.toArray());
        return count == null ? 0 : count;
    }

    @Override
    public boolean insertSubmission(CountSubmissionRecord submission) {
        return jdbcTemplate.update("""
                INSERT INTO inventory_count_submission (
                    id, session_id, operation_id, payload_hash, line_count, version_after,
                    recorded_by, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (session_id, operation_id) DO NOTHING
                """, submission.id(), submission.sessionId(), submission.operationId(),
                submission.payloadHash(), submission.lineCount(), submission.versionAfter(),
                submission.recordedBy(), submission.createdAt()) == 1;
    }

    @Override
    public int[][] insertObservations(List<CountObservationRecord> observations) {
        return jdbcTemplate.batchUpdate("""
                INSERT INTO inventory_count_observation (
                    id, submission_id, session_id, inventory_id, counted_good_quantity,
                    counted_defective_quantity, counted_pending_quantity, count_reason,
                    evidence_reference, counted_by, counted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, observations, observations.size(), (PreparedStatement statement,
                                                           CountObservationRecord observation) -> {
            statement.setString(1, observation.id());
            statement.setString(2, observation.submissionId());
            statement.setString(3, observation.sessionId());
            statement.setString(4, observation.inventoryId());
            statement.setBigDecimal(5, observation.countedGoodQuantity());
            statement.setBigDecimal(6, observation.countedDefectiveQuantity());
            statement.setBigDecimal(7, observation.countedPendingQuantity());
            statement.setString(8, observation.countReason());
            statement.setString(9, observation.evidenceReference());
            statement.setString(10, observation.countedBy());
            statement.setObject(11, observation.countedAt());
        });
    }

    @Override
    public boolean advanceVersion(String sessionId, long expectedVersion) {
        return jdbcTemplate.update("""
                UPDATE inventory_count_session
                SET version = version + 1
                WHERE id = ? AND version = ? AND status = 'OPEN'
                """, sessionId, expectedVersion) == 1;
    }

    /** Một dòng được coi là đã đếm nếu có ít nhất một observation; recount mới nhất được dùng khi tổng hợp. */
    @Override
    public long countUncountedLines(String sessionId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM inventory_count_session_line l
                WHERE l.session_id = ?
                  AND NOT EXISTS (
                      SELECT 1 FROM inventory_count_observation o
                      WHERE o.session_id = l.session_id AND o.inventory_id = l.inventory_id
                  )
                """, Long.class, sessionId);
        return count == null ? 0 : count;
    }

    /** Tính tổng variance baseline so với observation mới nhất, không ghi đè số dư tồn kho. */
    @Override
    public CountSummary summarizeLatestCounts(String sessionId) {
        return jdbcTemplate.queryForObject("""
                WITH latest AS (
                    SELECT DISTINCT ON (o.inventory_id)
                           o.inventory_id, o.counted_good_quantity,
                           o.counted_defective_quantity, o.counted_pending_quantity
                    FROM inventory_count_observation o
                    JOIN inventory_count_submission s ON s.id = o.submission_id
                    WHERE o.session_id = ?
                    ORDER BY o.inventory_id, s.version_after DESC, o.counted_at DESC, o.id DESC
                )
                SELECT COUNT(*) AS line_count,
                       COALESCE(SUM(c.counted_good_quantity - (l.physical_quantity
                           - l.defective_quantity - l.pending_quantity)), 0) AS good_variance,
                       COALESCE(SUM(c.counted_defective_quantity - l.defective_quantity), 0)
                           AS defective_variance,
                       COALESCE(SUM(c.counted_pending_quantity - l.pending_quantity), 0)
                           AS pending_variance
                FROM inventory_count_session_line l
                JOIN latest c ON c.inventory_id = l.inventory_id
                WHERE l.session_id = ?
                """, (rs, row) -> new CountSummary(rs.getLong("line_count"),
                rs.getBigDecimal("good_variance"), rs.getBigDecimal("defective_variance"),
                rs.getBigDecimal("pending_variance")), sessionId, sessionId);
    }

    /** Submit chỉ cập nhật trạng thái; bước duyệt sau mới được phép ghi nhận điều chỉnh vào tồn. */
    @Override
    public boolean submitSession(String sessionId, long expectedVersion) {
        return jdbcTemplate.update("""
                UPDATE inventory_count_session
                SET status = 'PENDING_APPROVAL', version = version + 1
                WHERE id = ? AND status = 'OPEN' AND version = ?
                """, sessionId, expectedVersion) == 1;
    }

    /** Khóa các balance bị ảnh hưởng theo inventory ID cố định để giảm nguy cơ deadlock khi duyệt. */
    @Override
    public List<CountApprovalLine> lockApprovalLines(String sessionId) {
        return jdbcTemplate.query("""
                WITH latest AS (
                    SELECT DISTINCT ON (o.inventory_id)
                           o.inventory_id, o.counted_good_quantity,
                           o.counted_defective_quantity, o.counted_pending_quantity, o.count_reason
                    FROM inventory_count_observation o
                    JOIN inventory_count_submission s ON s.id = o.submission_id
                    WHERE o.session_id = ?
                    ORDER BY o.inventory_id, s.version_after DESC, o.counted_at DESC, o.id DESC
                )
                SELECT l.inventory_id, l.inventory_version AS snapshot_version,
                       i.version AS current_version, i.physical_quantity AS current_physical,
                       i.reserved_quantity AS current_reserved, i.defective_quantity AS current_defective,
                       i.pending_quantity AS current_pending, c.counted_good_quantity,
                       c.counted_defective_quantity, c.counted_pending_quantity, c.count_reason
                FROM inventory_count_session_line l
                JOIN inventory i ON i.id = l.inventory_id
                JOIN latest c ON c.inventory_id = l.inventory_id
                WHERE l.session_id = ?
                ORDER BY l.inventory_id
                FOR UPDATE OF i
                """, (rs, row) -> new CountApprovalLine(rs.getString("inventory_id"),
                rs.getLong("snapshot_version"), rs.getLong("current_version"),
                rs.getBigDecimal("current_physical"), rs.getBigDecimal("current_reserved"),
                rs.getBigDecimal("current_defective"), rs.getBigDecimal("current_pending"),
                rs.getBigDecimal("counted_good_quantity"), rs.getBigDecimal("counted_defective_quantity"),
                rs.getBigDecimal("counted_pending_quantity"), rs.getString("count_reason")), sessionId);
    }

    @Override
    public boolean approveSession(String sessionId, long expectedVersion, String approvedBy,
                                 LocalDateTime approvedAt) {
        return jdbcTemplate.update("""
                UPDATE inventory_count_session
                SET status = 'APPROVED', version = version + 1, approved_by = ?, approved_at = ?
                WHERE id = ? AND status = 'PENDING_APPROVAL' AND version = ?
                """, approvedBy, approvedAt, sessionId, expectedVersion) == 1;
    }

    @Override
    public Optional<CountReviewRecord> findCountReview(String sessionId, String operationId) {
        List<CountReviewRecord> reviews = jdbcTemplate.query("""
                SELECT id, session_id, operation_id, payload_hash, reason, requested_by,
                       version_after, requested_at
                FROM inventory_count_review
                WHERE session_id = ? AND operation_id = ?
                """, (rs, row) -> new CountReviewRecord(rs.getString("id"), rs.getString("session_id"),
                rs.getString("operation_id"), rs.getString("payload_hash"), rs.getString("reason"),
                rs.getString("requested_by"), rs.getLong("version_after"),
                rs.getTimestamp("requested_at").toLocalDateTime()), sessionId, operationId);
        return reviews.stream().findFirst();
    }

    @Override
    public boolean insertCountReview(CountReviewRecord review) {
        return jdbcTemplate.update("""
                INSERT INTO inventory_count_review (
                    id, session_id, operation_id, payload_hash, reason, requested_by, version_after, requested_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (session_id, operation_id) DO NOTHING
                """, review.id(), review.sessionId(), review.operationId(), review.payloadHash(),
                review.reason(), review.requestedBy(), review.versionAfter(), review.requestedAt()) == 1;
    }

    /** Chỉ mở lại phiên đúng version; observations cũ được giữ làm lịch sử recount. */
    @Override
    public boolean reopenForRecount(String sessionId, long expectedVersion) {
        return jdbcTemplate.update("""
                UPDATE inventory_count_session
                SET status = 'OPEN', version = version + 1
                WHERE id = ? AND status = 'PENDING_APPROVAL' AND version = ?
                """, sessionId, expectedVersion) == 1;
    }

    @Override
    public boolean insertSession(InventoryCountSessionRecord session) {
        return jdbcTemplate.update("""
                INSERT INTO inventory_count_session (
                    id, enterprise_id, warehouse_id, location_id, sku, operation_id, payload_hash,
                    status, reason, opened_by, snapshot_at, version, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (enterprise_id, operation_id) DO NOTHING
                """, session.id(), session.enterpriseId(), session.warehouseId(), session.locationId(),
                session.sku(), session.operationId(), session.payloadHash(), session.status(), session.reason(),
                session.openedBy(), session.snapshotAt(), session.version(), session.createdAt()) == 1;
    }

    @Override
    public long snapshotInventory(InventoryCountSessionRecord session) {
        StringBuilder sql = new StringBuilder("""
                INSERT INTO inventory_count_session_line (
                    id, session_id, inventory_id, sku, location_id, physical_quantity, reserved_quantity,
                    defective_quantity, pending_quantity, in_transit_quantity, inventory_version
                )
                SELECT gen_random_uuid()::text, ?, i.id, i.sku, i.location_id, i.physical_quantity,
                       i.reserved_quantity, i.defective_quantity, i.pending_quantity,
                       i.in_transit_quantity, i.version
                FROM inventory i
                WHERE i.enterprise_id = ? AND i.warehouse_id = ?
                """);
        ArrayList<Object> args = new ArrayList<>(List.of(
                session.id(), session.enterpriseId(), session.warehouseId()));
        if (session.locationId() != null) {
            sql.append(" AND i.location_id = ?");
            args.add(session.locationId());
        }
        if (session.sku() != null) {
            sql.append(" AND i.sku = ?");
            args.add(session.sku());
        }
        // Snapshot lock is short: it prevents a balance update between reading the selected rows and commit.
        sql.append(" ORDER BY i.id FOR SHARE");
        return jdbcTemplate.update(sql.toString(), args.toArray());
    }
}
