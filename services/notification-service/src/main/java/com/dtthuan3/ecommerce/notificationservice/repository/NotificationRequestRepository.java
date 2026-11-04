package com.dtthuan3.ecommerce.notificationservice.repository;

import com.dtthuan3.ecommerce.notificationservice.entity.NotificationRequest;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, String> {

    boolean existsByBusinessKey(String businessKey);

    Optional<NotificationRequest> findByBusinessKey(String businessKey);

    /** Worker lay batch due, uu tien QUEUED/RETRY, gioi han batch de khong giu lock dai. */
    @Query("""
            SELECT r FROM NotificationRequest r
            WHERE r.status IN ('QUEUED','FAILED_RETRYABLE')
              AND (r.nextAttemptAt IS NULL OR r.nextAttemptAt <= :now)
            ORDER BY r.createdAt ASC
            """)
    List<NotificationRequest> findDueBatch(@Param("now") Instant now, @Param("limit") int unusedLimit);

    List<NotificationRequest> findByRecipientIdOrderByCreatedAtDesc(String recipientId);

    List<NotificationRequest> findByOrderIdOrderByCreatedAtDesc(String orderId);

    long countByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM NotificationRequest r WHERE r.id = :id")
    Optional<NotificationRequest> findByIdForUpdate(@Param("id") String id);
}
