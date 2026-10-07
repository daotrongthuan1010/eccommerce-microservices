package com.dtthuan3.ecommerce.reviewservice.entity;

import com.dtthuan3.ecommerce.reviewservice.entity.enums.ReviewMediaType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_media", indexes = {@Index(name = "idx_review_media_review", columnList = "review_id")})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; /* * Review chứa media. */
    @Column(name = "review_id", nullable = false)
    private Long reviewId; /* * IMAGE hoặc VIDEO. */
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private ReviewMediaType mediaType; /* * URL lấy từ MinIO/S3. * * Ví dụ: * * http://minio/review/abc.jpg */
    @Column(name = "media_url", nullable = false, length = 1000)
    private String mediaUrl; /* * Thumbnail dành cho video. */
    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl; /* * Thứ tự hiển thị. * * 0, 1, 2... */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}