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

/**
 * Cung cấp thao tác lưu, lấy và khóa các sự kiện outbox dùng để vô hiệu hóa cache sau commit.
 * Interface này là ranh giới truy cập dữ liệu cho service, gom các thao tác đọc/ghi cần thiết. Cách thực
 * thi bằng JPA hay JDBC nằm trong repository/adapter, không đặt trong controller.
 */
public interface CacheInvalidationOutboxRepository extends JpaRepository<CacheInvalidationOutboxEvent, String> {
    List<CacheInvalidationOutboxEvent> findTop100ByAvailableAtLessThanEqualOrderByCreatedAtAsc(Instant now);

    Optional<CacheInvalidationOutboxEvent> findFirstByOrderByCreatedAtAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from CacheInvalidationOutboxEvent event where event.id = :id")
    Optional<CacheInvalidationOutboxEvent> findByIdForUpdate(@Param("id") String id);
}
