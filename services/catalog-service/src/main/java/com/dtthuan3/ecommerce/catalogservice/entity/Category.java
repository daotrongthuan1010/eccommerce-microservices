package com.dtthuan3.ecommerce.catalogservice.entity;

import com.dtthuan3.ecommerce.catalogservice.contstant.CategoryStatus;
import com.dtthuan3.ecommerce.catalogservice.generator.StringUuidSequenceGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@Table(
        name = "categories",
        indexes = {
                @Index(name = "idx_categories_parent_id", columnList = "parent_id"),
                @Index(name = "idx_categories_slug", columnList = "slug", unique = true),
                @Index(name = "idx_categories_status", columnList = "status")
        }
)
@Getter
@Setter
public class Category extends BaseEntity {

    /** ID duy nhất của danh mục. */
    @Id
    @GeneratedValue(generator = "category_uuid_generator")
    @GenericGenerator(
            name = "category_uuid_generator",
            type = StringUuidSequenceGenerator.class,
            parameters = {
                    @Parameter(
                            name = "sequence_name",
                            value = "categories_id_seq"
                    )
            }
    )
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    /** ID của danh mục cha, null nếu đây là danh mục gốc. */
    @Column(name = "parent_id", length = 36)
    private String parentId;

    /** Tên danh mục. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Đường dẫn thân thiện dùng cho URL. */
    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    /** Mô tả danh mục. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Object key của ảnh danh mục trên MinIO. */
    @Column(name = "image_object_key", length = 500)
    private String imageObjectKey;

    /** Thứ tự hiển thị danh mục. */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    /** Trạng thái hoạt động của danh mục. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CategoryStatus status;

    /** Phiên bản  danh mục. */
    @Column(name = "revision", nullable = false)
    private Long revision = 0L;
}