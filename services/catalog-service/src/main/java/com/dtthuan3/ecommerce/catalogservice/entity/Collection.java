package com.dtthuan3.ecommerce.catalogservice.entity;

import com.dtthuan3.ecommerce.catalogservice.contstant.CollectionStatus;
import com.dtthuan3.ecommerce.catalogservice.generator.StringUuidSequenceGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "collections",
        indexes = {
                @Index(name = "idx_collections_slug", columnList = "slug", unique = true),
                @Index(name = "idx_collections_status", columnList = "status"),
                @Index(
                        name = "idx_collections_schedule",
                        columnList = "status,start_at,end_at"
                )

        }
)
@Getter
@Setter
public class Collection extends BaseEntity {

    /** ID duy nhất của bộ sưu tập. */
    @Id
    @GeneratedValue(generator = "collection_uuid_generator")
    @GenericGenerator(
            name = "collection_uuid_generator",
            type = StringUuidSequenceGenerator.class,
            parameters = {
                    @Parameter(
                            name = "sequence_name",
                            value = "collections_id_seq"
                    )
            }
    )
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    /** Tên bộ sưu tập. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Đường dẫn thân thiện dùng cho URL. */
    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    /** Mô tả bộ sưu tập. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Object key của banner trên MinIO. */
    @Column(name = "banner_object_key", length = 500)
    private String bannerObjectKey;

    /** Thời điểm bộ sưu tập bắt đầu có hiệu lực. */
    @Column(name = "start_at")
    private LocalDateTime startAt;

    /** Thời điểm bộ sưu tập kết thúc hiệu lực. */
    @Column(name = "end_at")
    private LocalDateTime endAt;

    /** Thứ tự hiển thị bộ sưu tập. */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    /** Trạng thái của bộ sưu tập. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CollectionStatus status;

    /** Phiên bản thay đổi của bộ sưu tập. */
    @Column(name = "revision", nullable = false)
    private Long revision = 0L;
}