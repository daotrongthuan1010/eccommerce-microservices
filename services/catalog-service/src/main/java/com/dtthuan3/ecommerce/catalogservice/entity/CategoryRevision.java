package com.dtthuan3.ecommerce.catalogservice.entity;


import com.dtthuan3.ecommerce.catalogservice.contstant.CategoryRevisionAction;
import com.dtthuan3.ecommerce.catalogservice.generator.StringUuidSequenceGenerator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@Table(
        name = "category_revisions",
        indexes = {
                @Index(name = "idx_category_revisions_category", columnList = "category_id"),
                @Index(name = "idx_category_revisions_revision", columnList = "revision")
        }
)
@Getter
@Setter
public class CategoryRevision extends BaseEntity {

    /** ID duy nhất của bản ghi lịch sử thay đổi danh mục. */
    @Id
    @GeneratedValue(generator = "category_revision_uuid_generator")
    @GenericGenerator(
            name = "category_revision_uuid_generator",
            type = StringUuidSequenceGenerator.class,
            parameters = {
                    @Parameter(
                            name = "sequence_name",
                            value = "category_revisions_id_seq"
                    )
            }
    )
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    /** ID danh mục được thay đổi. */
    @Column(name = "category_id", nullable = false, length = 36)
    private String categoryId;

    /** Số revision của thay đổi. */
    @Column(name = "revision", nullable = false)
    private Long revision;

    /** Loại hành động đã thực hiện trên danh mục. */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private CategoryRevisionAction action;

    /** ID danh mục cha trước khi thay đổi. */
    @Column(name = "old_parent_id", length = 36)
    private String oldParentId;

    /** ID danh mục cha sau khi thay đổi. */
    @Column(name = "new_parent_id", length = 36)
    private String newParentId;

    /** Slug trước khi thay đổi. */
    @Column(name = "old_slug", length = 255)
    private String oldSlug;

    /** Slug sau khi thay đổi. */
    @Column(name = "new_slug", length = 255)
    private String newSlug;
}