package com.dtthuan3.ecommerce.reviewservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/** Từ cấm quản trị được: review chứa từ này thì rơi vào chờ duyệt. */
@Entity
@Table(name = "banned_words",
        uniqueConstraints = @UniqueConstraint(name = "uk_banned_word", columnNames = {"word"}),
        indexes = @Index(name = "ix_banned_word", columnList = "word"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannedWord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Luôn lưu đã chuẩn hóa: trim + lowercase. */
    @Column(length = 100, nullable = false, unique = true)
    private String word;

    @Column(length = 255)
    private String note;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
