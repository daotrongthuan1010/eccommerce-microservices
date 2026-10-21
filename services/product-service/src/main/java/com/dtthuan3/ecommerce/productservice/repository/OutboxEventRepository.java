
package com.dtthuan3.ecommerce.productservice.repository;

import com.dtthuan3.ecommerce.productservice.constant.OutboxStatus;
import com.dtthuan3.ecommerce.productservice.domain.OutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    @Query("""
            select e.id
            from OutboxEvent e
            where e.status = :status
              and (
                    e.nextRetryAt is null
                    or e.nextRetryAt <= :now
              )
            order by e.occurredAt asc, e.id asc
            """)
    List<UUID> findPendingEventIds(
            @Param("status") OutboxStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e
            from OutboxEvent e
            where e.id = :id
            """)
    Optional<OutboxEvent> findByIdForUpdate(
            @Param("id") UUID id
    );
}
