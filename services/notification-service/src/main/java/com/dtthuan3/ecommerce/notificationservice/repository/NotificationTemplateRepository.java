package com.dtthuan3.ecommerce.notificationservice.repository;

import com.dtthuan3.ecommerce.notificationservice.entity.NotificationTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {

    /** Version ACTIVE moi nhat theo key + channel + locale (de chot version cho request moi). */
    Optional<NotificationTemplate> findFirstByTemplateKeyAndChannelAndLocaleAndStatusOrderByVersionDesc(
            String templateKey, String channel, String locale, String status);

    /** Lay dung version da chot de render lai dung noi dung da duyet. */
    Optional<NotificationTemplate> findByTemplateKeyAndChannelAndVersion(
            String templateKey, String channel, int version);
}
