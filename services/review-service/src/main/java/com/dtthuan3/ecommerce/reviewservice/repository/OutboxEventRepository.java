package com.dtthuan3.ecommerce.reviewservice.repository;

import com.dtthuan3.ecommerce.reviewservice.entity.OutboxEvent;
import com.dtthuan3.ecommerce.reviewservice.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    @Query(value = "SELECT * FROM outbox_event WHERE status = 'PENDING' "
            + "ORDER BY created_at ASC, aggregate_version ASC LIMIT :limit FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<OutboxEvent> lockPending(@Param("limit") int limit);

    long countByStatus(OutboxStatus status);

    java.util.Optional<OutboxEvent> findFirstByStatusOrderByCreatedAtAsc(OutboxStatus status);

    @Modifying
    @Query("delete from OutboxEvent e where e.status = :status and e.sentAt < :before")
    int deleteSentBefore(@Param("status") OutboxStatus status, @Param("before") LocalDateTime before);
}