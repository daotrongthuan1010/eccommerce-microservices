package com.dtthuan3.ecommerce.notificationservice.repository;

import com.dtthuan3.ecommerce.notificationservice.entity.NotificationDeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryAttemptRepository extends JpaRepository<NotificationDeliveryAttempt, String> {
    int countByRequestId(String requestId);
}
