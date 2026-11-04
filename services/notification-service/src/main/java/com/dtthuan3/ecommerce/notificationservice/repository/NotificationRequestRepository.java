package com.dtthuan3.ecommerce.notificationservice.repository;

import com.dtthuan3.ecommerce.notificationservice.entity.NotificationRequest;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, String> {

    boolean existsByBusinessKey(String businessKey);

    Optional<NotificationRequest> findByBusinessKey(String businessKey);

    /** Worker lay batch due, uu tien QUEUED/RETRY. LIMIT trong DB de khong load
     *  ca hang tram nghin row vao RAM khi tai lon. */
    @Query("""
            SELECT r FROM NotificationRequest r
            WHERE r.status IN ('QUEUED','FAILED_RETRYABLE')
              AND (r.nextAttemptAt IS NULL OR r.nextAttemptAt <= :now)
            ORDER BY r.createdAt ASC
            """)
    List<NotificationRequest> findDueBatch(@Param("now") Instant now, Pageable pageable);

    /** Recovery: pod chet giu transaction de request kec SENDING — worker
     *  sau khi khoi dong lai lay nhung request SENDING "ma" qua :staleAfter
     *  de xu ly lai (idempotent theo businessKey + attempt history). */
    @Query("""
            SELECT r FROM NotificationRequest r
            WHERE r.status = 'SENDING'
              AND r.updatedAt <= :staleAfter
            ORDER BY r.updatedAt ASC
            """)
    List<NotificationRequest> findStaleSending(@Param("staleAfter") Instant staleAfter, Pageable pageable);

    List<NotificationRequest> findByRecipientIdOrderByCreatedAtDesc(String recipientId);

    List<NotificationRequest> findByOrderIdOrderByCreatedAtDesc(String orderId);

    long countByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM NotificationRequest r WHERE r.id = :id")
    Optional<NotificationRequest> findByIdForUpdate(@Param("id") String id);
}
