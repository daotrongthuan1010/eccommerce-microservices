package com.dtthuan3.ecommerce.notificationservice.repository;

import com.dtthuan3.ecommerce.notificationservice.entity.NotificationDeadLetter;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeadLetterRepository extends JpaRepository<NotificationDeadLetter, String> {
    List<NotificationDeadLetter> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
}
