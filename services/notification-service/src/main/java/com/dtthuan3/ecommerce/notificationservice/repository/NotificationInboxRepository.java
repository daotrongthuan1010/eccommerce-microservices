package com.dtthuan3.ecommerce.notificationservice.repository;

import com.dtthuan3.ecommerce.notificationservice.entity.NotificationInbox;
import com.dtthuan3.ecommerce.notificationservice.entity.NotificationInboxId;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationInboxRepository extends JpaRepository<NotificationInbox, NotificationInboxId> {
    boolean existsByEventIdAndSource(String eventId, String source);
}
