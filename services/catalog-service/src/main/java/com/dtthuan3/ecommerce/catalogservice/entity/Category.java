package com.dtthuan3.ecommerce.catalogservice.entity;

import com.dtthuan3.ecommerce.catalogservice.contstant.CategoryStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table( name = "categories",
        indexes = {
                @Index(
                        name = "idx_categories_parent_id",
                        columnList = "parent_id"
                ),
                @Index(
                        name = "idx_categories_slug",
                        columnList = "slug"
                ),
                @Index(
                        name = "idx_categories_status",
                        columnList = "status"
                )
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(
            name = "category_seq",
            sequenceName = "categories_id_seq",
            allocationSize = 50
    )
    private Long id;

    // id danh mục cha
    @Column(name = "parent_id")
    private Long parentId;

    // tên danh mục
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    // url
    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    // mô tả sản phẩm
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // URL hình ảnh danh mục
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // thứ tự hiển thị
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    // trạng thái danh mục
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CategoryStatus status = CategoryStatus.ACTIVE;

}
