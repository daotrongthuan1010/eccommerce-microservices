package com.dtthuan3.ecommerce.reviewservice.repository;

import com.dtthuan3.ecommerce.reviewservice.entity.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface InboxEventRepository extends JpaRepository<InboxEvent, String> {
    boolean existsByConsumerNameAndEventId(String consumerName, String eventId);

    @Modifying
    @Query("delete from InboxEvent e where e.processedAt < :before")
    int deleteProcessedBefore(@Param("before") LocalDateTime before);
}
