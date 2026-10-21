package com.dtthuan3.ecommerce.catalogservice.entity;

import com.dtthuan3.ecommerce.catalogservice.contstant.CategoryStatus;
import com.dtthuan3.ecommerce.catalogservice.generator.StringUuidSequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@Table(
        name = "categories",
        indexes = {
                @Index(
                        name = "idx_categories_parent_id",
                        columnList = "parent_id"
                ),
                @Index(
                        name = "idx_categories_slug_parent",
                        columnList = "slug,parent_id",
                        unique = true
                ),
                @Index(
                        name = "idx_categories_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
public class Category extends BaseEntity {

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
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            length = 36
    )
    private String id;

    @Column(name = "parent_id", length = 36)
    private String parentId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, length = 255)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_object_key", length = 500)
    private String imageObjectKey;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CategoryStatus status = CategoryStatus.ACTIVE;

    @Column(name = "revision", nullable = false)
    private Long revision = 0L;
}