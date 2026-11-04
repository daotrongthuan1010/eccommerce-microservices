package com.dtthuan3.ecommerce.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Mau thong bao co phien ban (NOTI-02): key + version + channel + locale.
 * Chi trang thai ACTIVE duoc dung de gui; sua mau = tao version moi, khong
 * update noi dung version da duyet (NOTI-UC04 buoc 4-6: version cu duoc giu).
 */
@Getter
@Setter
@Entity
@Table(name = "notification_template",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_template",
                columnNames = {"template_key", "channel", "version"}),
        indexes = @Index(name = "idx_notification_template_lookup",
                columnList = "template_key, channel, locale, status"))
public class NotificationTemplate {
    @Id @Column(name = "id", length = 36) private String id;
    @Column(name = "template_key", nullable = false, length = 64) private String templateKey;
    @Column(nullable = false) private int version;
    @Column(nullable = false, length = 16) private String channel;   // EMAIL
    @Column(nullable = false, length = 8) private String locale;     // vi
    @Column(nullable = false, length = 16) private String purpose;   // TRANSACTIONAL | MARKETING
    @Column(name = "subject_template", nullable = false, length = 255) private String subjectTemplate;
    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT") private String bodyTemplate;
    /** Bien bat buoc, phan cach bang dau phay. Thieu bien = khong gui (NOTI-UC04). */
    @Column(name = "required_variables", nullable = false, length = 255) private String requiredVariables;
    @Column(nullable = false, length = 16) private String status;    // DRAFT | ACTIVE | RETIRED
    @Column(name = "created_by", nullable = false, length = 64) private String createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = Instant.now(); }
}
