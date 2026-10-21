package com.dtthuan3.ecommerce.inventoryservice.repository;

import com.dtthuan3.ecommerce.inventoryservice.entity.CacheInvalidationOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CacheInvalidationOutboxRepository extends JpaRepository<CacheInvalidationOutboxEvent, String> {
    List<CacheInvalidationOutboxEvent> findTop100ByAvailableAtLessThanEqualOrderByCreatedAtAsc(Instant now);

    Optional<CacheInvalidationOutboxEvent> findFirstByOrderByCreatedAtAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from CacheInvalidationOutboxEvent event where event.id = :id")
    Optional<CacheInvalidationOutboxEvent> findByIdForUpdate(@Param("id") String id);
}
